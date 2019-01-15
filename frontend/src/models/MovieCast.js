var _ = require('lodash');

function MovieCast(title, uuid, cast) {
  _.extend(this, {
    title: title,
    uuid : uuid,
    cast: cast.map(function (c) {
      return {
        name: c[0],
        cast_uuid: c[1],
        relation_uuid : c[2],
        job: c[3],
        role: c[4]
      }
    })
  });
}

module.exports = MovieCast;
