import 'bootstrap';
import './scss/app.scss';
import * as $ from 'jquery'

const api = require('./neo4jApi');
const exonum = require('./exonumApi');
const d3 = require("d3");

const historyCard = $('#node-history-card');
const transactionCard = $('#transaction-card');
const movieCard = $('#movie-card');
const graphCard = $('#graph');
const searchCard = $('#search-card');
const blocksCard = $('#blocks-card');
const blockCard = $('#block-card');

$(function () {
    renderGraph();
    search();
    showStats();
    getBlocks();

    $("#movie-form").submit(e => {
        e.preventDefault();
        search();
    });

    $("#blocks-form").submit(e => {
        e.preventDefault();
        getBlocks();
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

    removeEventHandlers(movieCard);

    api
        .getMovie(title)
        .then(movie => {
            if (!movie) return;
            $("#title").html(movie.title + "<span class='block exonum-clickable float-right' data-toggle=\"tooltip\" data-placement=\"right\" data-uuid=\"" + movie.uuid + "\" title=\"" + movie.uuid + "\">" + shortUUID(movie.uuid) + "</span>");

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

                initEventHandlers(movieCard);

            } else {
                $("<tr><td colspan='4'>No crew found</td></tr>").appendTo(t);
            }
        }, "json");
}

function search() {

    removeEventHandlers(searchCard);

    const query = $("#movie-form").find("input[name=search]").val();
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

                    initEventHandlers(searchCard);

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

function getBlocks() {

    removeEventHandlers(blocksCard);

    const numberOfBlocks = $("#blocks-form").find("select[name=amount]").val();
    const skipEmpty = $("#blocks-form").find("input[name=skip]").is(':checked');

    $.when(exonum.getBlocks(numberOfBlocks, false, skipEmpty)).then(data => {

        if (data) {

            const t = $("table#blocks-table tbody").empty();

            if (data.blocks.length > 0) {

                data.blocks.forEach(block => {
                    $("<tr>" +
                        "<td>" + block.height + "</td>" +
                        "<td class='exonum-clickable' data-toggle=\"tooltip\" data-placement=\"right\"  title=\"" + block.state_hash + "\">" + shortTX(block.state_hash, 8) + "</td>" +
                        "<td>" + block.tx_count + "</td>" +
                        "</tr>").appendTo(t)
                        .click(function () {
                            showBlock(block.height);
                        });
                });

                let first = data.blocks[0];
                if (first) {
                    showBlock(first.height);
                }

                initEventHandlers(blocksCard);
            } else {
                $("<tr><td colspan='3'>No blocks found.</td></tr>").appendTo(t)
            }
        }
    })
}

function showBlock(height) {

    removeEventHandlers(blockCard);

    $("#block-card-header").html("Block" + "<span class='float-right'>" + height + "</span>");

    $.when(exonum.getBlock(height)).then(data => {

        const t = $("table#block-table tbody").empty();

        if (data.txs) {

            data.txs.forEach(tx => {
                $("<tr>" +
                    "<th>TX</th>" +
                    "<td class='transaction exonum-clickable' data-tx=\"" + tx + "\" data-toggle=\"tooltip\" data-placement=\"right\"  title=\"" + tx + "\">" + shortTX(tx, 20) + "</td>" +
                    "</tr>").appendTo(t)
            });

            initEventHandlers(blockCard);
        } else {
            $("<tr><td>No transactions</td></tr>").appendTo(t)
        }

        $('html,body').animate({
            scrollTop: blockCard.offset().top
        });
    });
}


function sendQuery() {

    const form = $("#execute_query");

    form.find(":input").prop("disabled", true);
    form.find(":input[name=send]").html("" +
        "<span class=\"spinner-grow spinner-grow-sm\" role=\"status\" aria-hidden=\"true\"></span>" +
        "<span> Waiting for confirmation</span>"
    );

    const query = $("#execute_query").find("input[name=query]").val();

    $.when(exonum.sendTransaction(query)).then(data => {
        waitForAcceptance(data.tx_hash).then(() => {

            if (data) {
                showTransaction(data.tx_hash);
            }
            $("#execute_query").find("input[name=query]").val("");
            $("#execute_query").find(":input").prop("disabled", false);
            $("#execute_query").find(":input[name=send]").html("Send Query");
        });
    });

}

function waitForAcceptance(hash) {

    let attempt = 100;

    return (function makeAttempt() {
        return exonum.getTransaction(hash).then(data => {

            if (typeof data.result === "undefined") {
                if (--attempt > 0) {
                    return new Promise(resolve => {
                        setTimeout(resolve, 100)
                    }).then(makeAttempt);
                } else {
                    throw new Error('Transaction has not been found');
                }
            } else {
                return data;
            }
        });
    })()
}


function showTransaction(hash) {

    removeEventHandlers(transactionCard);

    $.when(exonum.getTransaction(hash)).then(data => {
        if (data) {

            $("table#transactions-table tbody").empty().append(
                "<tr><th>Hash</th><td data-toggle=\"tooltip\" data-tx='" + hash + "' title='" + hash + "' class='transaction exonum-clickable'>" + hash + "</td></tr>" +
                "<tr><th>Sender public key</th><td>" + data.pub_key + "</td></tr>" +
                "<tr><th>Status</th><td>" + data.result + "</td></tr>" +
                "<tr><th>Content</th><td>" + data.queries + "</td></tr>" +
                "<tr><th>Error</th><td>" + data.error_msg + "</td></tr>"
            );

            $('html,body').animate({
                scrollTop: $("#transactions-table").offset().top
            });

            initEventHandlers(transactionCard);
        }
    })
}

function showNodeHistory(uuid) {

    removeEventHandlers(historyCard)

    $("#node-history-uuid").html("<span class='uuid exonum-clickable float-right' data-toggle=\"tooltip\" " +
        "data-placement=\"right\" data-uuid=\"" + uuid + "\" title=\"" + uuid + "\">" + shortUUID(uuid) + "</span>");

    $.when(exonum.getNodeHistory(uuid)).then(data => {

        const t = $("table#node-history tbody").empty();

        if (data) {
            data.forEach(change => {
                $("<tr>" +
                    "<td  data-toggle=\"tooltip\" title='" + change.transaction_id + "' data-tx='" + change.transaction_id + "' class='transaction exonum-clickable'>" + shortTX(change.transaction_id, 3) + "</td>" +
                    "<td >" + change.description + "</td>" +
                    "</tr>").appendTo(t)
            });

            $('html,body').animate({
                scrollTop: $("#node-history").offset().top
            });

            initEventHandlers(historyCard)
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

            let link = svg.selectAll(".link")
                .data(graph.links).enter()
                .append("line").attr("class", "link");

            let node = svg.selectAll(".node")
                .data(graph.nodes).enter()
                .append("circle")
                .attr("class", d => {
                    return "node uuid " + d.label
                })
                .attr("r", 10)
                .call(force.drag);

            // html title attribute
            node.attr("data-toggle", "tooltip")
                .attr("title", d => {
                    return d.title;
                });

            // UUID onclick
            node.attr("data-uuid", d => d.uuid);

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

            initEventHandlers(graphCard);

        });


}

function removeEventHandlers(container) {
    container.find('[data-toggle="tooltip"]').tooltip('dispose');
    container.find('.transaction').off();
    container.find('.uuid').off();
}

function initEventHandlers(container) {

    $(function () {
        container.find('[data-toggle="tooltip"]').tooltip();
        container.find('.transaction').click(function () {
            showTransaction($(this).attr('data-tx'));
        });
        container.find('.uuid').click(function () {
            showNodeHistory($(this).attr('data-uuid'));
        });
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

function shortTX(tx, numberOfChars) {
    return tx.substr(0, numberOfChars) + "..." + tx.substr(-numberOfChars, numberOfChars);
}
