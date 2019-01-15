var api = require('./neo4jApi');

$(function () {
  renderGraph();
  search();
  showStats();

  $("#search").submit(e => {
    e.preventDefault();
    search();
  });

  $("#stats_toggle").change(function() {
      if(this.checked) {
          $("#stats_container").show();
      } else {
          $("#stats_container").hide();
      }
  });
});

function showMovie(title) {
  api
    .getMovie(title)
    .then(movie => {
      if (!movie) return;
      $("#title").html(movie.title + "<span style='float:right;' title=\"" + movie.uuid + "\">[" + shortUUID(movie.uuid) + "]</span>");
      $("#poster").attr("src", "http://neo4j-contrib.github.io/developer-resources/language-guides/assets/posters/" + movie.title + ".jpg");

      var t = $("table#crew tbody").empty();

      movie.cast.forEach(cast => {
        $("<tr>"
            + "<td>" + cast.name +  "</td>"
            + "<td title=\"" + cast.cast_uuid + "\">[" + shortUUID(cast.cast_uuid) + "]</td> "
            + "<td>" + cast.job + (cast.job == "acted" ? " as " + cast.role : "") + "</td>"
            + "<td title=\"" + cast.relation_uuid + "\">[" + shortUUID(cast.rellation_uuid) + "]</td>"
            + "</tr>").appendTo(t);
      });
    }, "json");
}

function search() {
  var query = $("#search").find("input[name=search]").val();
  api
    .searchMovies(query)
    .then(movies => {
      var t = $("table#results tbody").empty();

      if (movies) {
        movies.forEach(movie => {
          $("<tr><td class='movie'>" + movie.title + "</td><td>" + movie.released + "</td><td>" + movie.tagline + "</td><td title=\"" + movie.uuid + "\">" + shortUUID(movie.uuid) + "</td></tr>").appendTo(t)
            .click(function() {
              showMovie($(this).find("td.movie").text());
            })
        });

        var first = movies[0];
        if (first) {
          showMovie(first.title);
        }
      }
    });
}

function showStats() {
    api.getStats()
        .then(labels => {
            var t = $("table#stats tbody").empty();

            if (labels) {
                labels.forEach(label => {
                    $("<tr>" +
                        "<td class='stats'>" + label.labels + "</td>" +
                        "<td>" + label.numOfNodes + "</td>" +
                        "<td>" + label.avgNumOfPropPerNode + "</td>" +
                        "<td>" + label.minNumPropPerNode + "</td>" +
                        "<td>" + label.maxNumPropPerNode + "</td>" +
                        "<td>" + label.avgNumOfRelationships + "</td>" +
                        "<td>" + label.minNumOfRelationships + "</td>" +
                        "<td>" + label.maxNumOfRelationships + "</td>" +
                        "</tr>").appendTo(t)
                });
            }
            setTimeout(function(){showStats();}, 2000);
        });
}

function shortUUID(uuid) {

    if( uuid ) {
        var uuid_parts = uuid.split('_');
        if (uuid_parts.length == 2) {
            return uuid_parts[0].substr(0, 3) + "..." + uuid_parts[0].substr(-3, 3) + "_" + uuid_parts[1];
        }
    }
    return uuid;
}

function renderGraph() {
  var width = 800, height = 800;
  var force = d3.layout.force()
    .charge(-200).linkDistance(30).size([width, height]);

  var svg = d3.select("#graph").append("svg")
    .attr("width", "100%").attr("height", "100%")
    .attr("pointer-events", "all");

  api
    .getGraph()
    .then(graph => {
      force.nodes(graph.nodes).links(graph.links).start();

      var link = svg.selectAll(".link")
        .data(graph.links).enter()
        .append("line").attr("class", "link");

      var node = svg.selectAll(".node")
        .data(graph.nodes).enter()
        .append("circle")
        .attr("class", d => {
          return "node " + d.label
        })
        .attr("r", 10)
        .call(force.drag);

      // html title attribute
      node.append("title")
        .text(d => {
          return d.title;
        });

      // force feed algo ticks
      force.on("tick", () => {
        link.attr("x1", d => {
          return d.source.x;
        }).attr("y1", d => {
          return d.source.y;
        }).attr("x2", d => {
          return d.target.x;
        }).attr("y2", d => {
          return d.target.y;
        });

        node.attr("cx", d => {
          return d.x;
        }).attr("cy", d => {
          return d.y;
        });
      });
    });
}
