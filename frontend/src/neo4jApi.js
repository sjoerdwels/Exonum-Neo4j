
const neo4j = require('neo4j-driver').v1;
const Movie = require('./models/Movie');
const MovieCast = require('./models/MovieCast');
const LabelStats = require('./models/LabelStats');
const _ = require('lodash');

const driver = neo4j.driver("bolt://" + process.env.NEO4J_BOLT_ADDRESS + ":" + + process.env.NEO4J_BOLT_PORT, neo4j.auth.basic(process.env.NEO4J_USERNAME, process.env.NEO4J_PASSWORD));

driver.onError = function() {
    console.log("Could not connect to Neo4j.");
};

const session = driver.session();

function searchMovies(queryString) {
  return session
    .run(
      'MATCH (movie:Movie) \
      WHERE movie.title =~ {title} \
      RETURN movie',
      {title: '(?i).*' + queryString + '.*'}
    )
    .then(result => {
      return result.records.map(record => {
        return new Movie(record.get('movie'));
      });
    })
    .catch(error => {
      throw error;
    });
}

function getMovie(title) {
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

      if (_.isEmpty(result.records))
        return null;

      var record = result.records[0];
      return new MovieCast(record.get('title'), record.get('uuid'), record.get('cast'));
    })
    .catch(error => {
      throw error;
    });
}

function getGraph() {

  return session.run(
    'MATCH (m)<-[:ACTED_IN]-(a:Person) \
    RETURN m.title AS movie, m.uuid AS uuid, collect([a.name, a.uuid]) AS cast \
    LIMIT {limit}', {limit: 100})
    .then(results => {
      let nodes = [], rels = [], i = 0;
      results.records.forEach(res => {
        nodes.push({title: res.get('movie'), uuid: res.get('uuid'), label: 'movie'});
        let target = i;
        i++;

        res.get('cast').forEach(name => {
          let actor = {title: name[0], uuid: name[1], label: 'actor'};
          let source = _.findIndex(nodes, actor);
          if (source === -1) {
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
            throw error;
        });
}

exports.searchMovies = searchMovies;
exports.getMovie = getMovie;
exports.getGraph = getGraph;
exports.getStats = getStats;

