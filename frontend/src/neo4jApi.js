require('file?name=[name].[ext]!../node_modules/neo4j-driver/lib/browser/neo4j-web.min.js');
var Movie = require('./models/Movie');
var MovieCast = require('./models/MovieCast');
var LabelStats = require('./models/LabelStats');
var _ = require('lodash');

var neo4j = window.neo4j.v1;
var driver = neo4j.driver("bolt://localhost", neo4j.auth.basic("neo4j", "1234"));

function searchMovies(queryString) {
  var session = driver.session();
  return session
    .run(
      'MATCH (movie:Movie) \
      WHERE movie.title =~ {title} \
      RETURN movie',
      {title: '(?i).*' + queryString + '.*'}
    )
    .then(result => {
      session.close();
      return result.records.map(record => {
        return new Movie(record.get('movie'));
      });
    })
    .catch(error => {
      session.close();
      throw error;
    });
}

function getMovie(title) {
  var session = driver.session();
  return session
    .run(
      "MATCH (movie:Movie {title:{title}}) \
      OPTIONAL MATCH (movie)<-[r]-(person:Person) \
      RETURN movie.title AS title, \
      movie.uuid AS uuid, \
      collect([person.name, \
           person.uuid, \
           r.uuid, \
           head(split(lower(type(r)), '_')), r.roles]) AS cast \
      LIMIT 1", {title})
    .then(result => {
      session.close();

      if (_.isEmpty(result.records))
        return null;

      var record = result.records[0];
      return new MovieCast(record.get('title'), record.get('uuid'), record.get('cast'));
    })
    .catch(error => {
      session.close();
      throw error;
    });
}

function getGraph() {
  var session = driver.session();
  return session.run(
    'MATCH (m:Movie)<-[:ACTED_IN]-(a:Person) \
    RETURN m.title AS movie, collect(a.name) AS cast \
    LIMIT {limit}', {limit: 100})
    .then(results => {
      session.close();
      var nodes = [], rels = [], i = 0;
      results.records.forEach(res => {
        nodes.push({title: res.get('movie'), label: 'movie'});
        var target = i;
        i++;

        res.get('cast').forEach(name => {
          var actor = {title: name, label: 'actor'};
          var source = _.findIndex(nodes, actor);
          if (source == -1) {
            nodes.push(actor);
            source = i;
            i++;
          }
          rels.push({source, target})
        })
      });

      return {nodes, links: rels};
    });
}

function getStats() {
    var session = driver.session();
    return session
        .run("MATCH (n)\n" +
            "RETURN\n" +
            "DISTINCT labels(n) AS Labels,\n" +
            "count(*) AS NumofNodes,\n" +
            "    avg(size(keys(n))) AS AvgNumOfPropPerNode,\n" +
            "    min(size(keys(n))) AS MinNumPropPerNode,\n" +
            "    max(size(keys(n))) AS MaxNumPropPerNode,\n" +
            "    avg(size((n)-[]-())) AS AvgNumOfRelationships,\n" +
            "    min(size((n)-[]-())) AS MinNumOfRelationships,\n" +
            "    max(size((n)-[]-())) AS MaxNumOfRelationships")

        .then(result => {
            session.close();

            console.log(result);

            return result.records.map(record => {
                return new LabelStats(
                    record.get('Labels'),
                    record.get('NumofNodes'),
                    record.get('AvgNumOfPropPerNode'),
                    record.get('MinNumPropPerNode'),
                    record.get('MaxNumPropPerNode'),
                    record.get('AvgNumOfRelationships'),
                    record.get('MinNumOfRelationships'),
                    record.get('MaxNumOfRelationships')
                );
            });
        })
        .catch(error => {
            session.close();
            throw error;
        });
}

exports.searchMovies = searchMovies;
exports.getMovie = getMovie;
exports.getGraph = getGraph;
exports.getStats = getStats;

