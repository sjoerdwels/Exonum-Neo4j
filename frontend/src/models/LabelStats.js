var _ = require('lodash');

function LabelStats(
    labels,
    numOfNodes,
    avgNumOfPropPerNode,
    minNumPropPerNode,
    maxNumPropPerNode,
    avgNumOfRelationships,
    minNumOfRelationships,
    maxNumOfRelationships,
    ) {
  _.extend(this, {
    labels : labels,
    numOfNodes : numOfNodes,
    avgNumOfPropPerNode : avgNumOfPropPerNode.toFixed(2),
    minNumPropPerNode : minNumPropPerNode.toNumber().toFixed(2),
    maxNumPropPerNode : maxNumPropPerNode.toNumber().toFixed(2),
    avgNumOfRelationships : avgNumOfRelationships.toFixed(2),
    minNumOfRelationships : minNumOfRelationships.toNumber().toFixed(2),
    maxNumOfRelationships : maxNumOfRelationships.toNumber().toFixed(2),
  });
}

module.exports = LabelStats;