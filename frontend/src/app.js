import 'bootstrap';
import './scss/app.scss';
import * as $ from 'jquery'

const api = require('./neo4jApi');
const exonum = require('./exonumApi');
const d3 = require("d3");

$(function () {
    renderGraph();
    search();
    showStats();

    $("#search").submit(e => {
        e.preventDefault();
        search();
    });

    $("#execute_query").submit(e => {
        e.preventDefault();
        sendQuery();
    });

    $("#graph-reload").on("click", () => {
        renderGraph();
    });

});

function showMovie(title) {
    api
        .getMovie(title)
        .then(movie => {
            if (!movie) return;
            $("#title").html(movie.title + "<span style='float:right;' title=\"" + movie.uuid + "\">[" + shortUUID(movie.uuid) + "]</span>");
            $("#poster").attr("src", "http://neo4j-contrib.github.io/developer-resources/language-guides/assets/posters/" + movie.title + ".jpg");

            const t = $("table#crew tbody").empty();

            if (movie.cast[0].name) {
                movie.cast.forEach(cast => {
                    $("<tr>"
                        + "<td>" + cast.name + "</td>"
                        + "<td  data-toggle=\"tooltip\" data-placement=\"right\"  class='uuid exonum-clickable'  data-uuid=\"" + cast.cast_uuid + "\" title=\"" + cast.cast_uuid + "\">" + shortUUID(cast.cast_uuid) + "</td> "
                        + "<td>" + cast.job + (cast.job === "acted" ? " as " + cast.role : "") + "</td>"
                        + "<td  data-toggle=\"tooltip\" data-placement=\"right\"  title=\"" + cast.relation_uuid + "\">" + shortUUID(cast.relation_uuid) + "</td>"
                        + "</tr>").appendTo(t);
                });

                $('.uuid').click(function () {
                    showNodeHistory($(this).attr('data-uuid'));
                });


                $(function () {
                    $('[data-toggle="tooltip"]').tooltip()
                });

            } else {
                $("<tr><td colspan='4'>No crew found</td></tr>").appendTo(t);
            }
        }, "json");
}

function search() {
    const query = $("#search").find("input[name=search]").val();
    api
        .searchMovies(query)
        .then(movies => {
            const t = $("table#results tbody").empty();

            if (movies) {
                if (movies.length > 0) {
                    movies.forEach(movie => {
                        $("<tr>" +
                            "<td class='movie'>" + movie.title + "</td>" +
                            "<td>" + movie.released + "</td><td>" + movie.tagline + "</td>" +
                            "<td class='uuid exonum-clickable' data-toggle=\"tooltip\" data-placement=\"right\"  data-uuid=\"" + movie.uuid + "\" title=\"" + movie.uuid + "\">" + shortUUID(movie.uuid) + "</td></tr>").appendTo(t)
                            .click(function () {
                                showMovie($(this).find("td.movie").text());
                            });
                    });

                    $('.uuid').click(function () {
                        console.log($(this));
                        showNodeHistory($(this).attr('data-uuid'));
                    });


                    $(function () {
                        $('[data-toggle="tooltip"]').tooltip()
                    });

                    let first = movies[0];
                    if (first) {
                        showMovie(first.title);
                    }
                } else {
                    $("<tr><td colspan='4'>No movies found</td></tr>").appendTo(t);
                }
            }
        });
}


function sendQuery() {
    const query = $("#execute_query").find("input[name=query]").val();
    try {
        $.when(exonum.sendTransaction(query)).then(data => {
            console.log(data);
        });
    } catch (error) {
        console.log(error);
    }
}

function showTransaction(transaction) {
    $.when(exonum.getTransaction(this.hash)).then( data => {
        console.log(data);
    })

    // this.transaction = data.content
    // this.location = data.location
    // this.status = data.status
    // this.type = data.type
    // this.isSpinnerVisible = false
}

function showNodeHistory(uuid) {

    $.when(exonum.getNodeHistory(uuid)).then(data => {

        const t = $("table#node-history tbody").empty();

        if (data) {
            data.forEach(change => {
                $("<tr>" +
                    "<td class='tx'> TX </td>" +
                    "<td >" + change + "</td>" +
                    "</tr>").appendTo(t)
            });

            $('html,body').animate({
                scrollTop: $("#node-history").offset().top
            })
        }
    });
}

function showStats() {
    api.getStats()
        .then(labels => {
            const t = $("table#stats tbody").empty();

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
            setTimeout(function () {
                showStats();
            }, 2000);
        });
}

function shortUUID(uuid) {

    if (uuid) {
        let uuid_parts = uuid.split('_');
        if (uuid_parts.length === 2) {
            return uuid_parts[0].substr(0, 3) + "..." + uuid_parts[0].substr(-3, 3) + "_" + uuid_parts[1];
        }
    }
    return uuid;
}

function renderGraph() {
    const width = 600, height = 600;
    const force = d3.layout.force()
        .charge(-50).linkDistance(50).size([width, height]);

    const svg = d3.select("#graph").html("").append("svg")
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
            node.attr("data-toggle", "tooltip")
                .attr("title", d => {
                    return d.title;
                });

            // UUID onclick
            node.attr("uuid", d => d.uuid)
                .on("click", function () {
                    showNodeHistory(d3.select(this).attr("uuid"));
                    d3.event.stopPropagation();
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

    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    });
}
