import tornado.ioloop
import tornado.web

from graphserver.ext.osm.osmdb import OSMDB
from graphserver.graphdb import GraphDatabase

OSMDB_FN = "/home/bmander/data/boston.osmdb"
GRAPHDB_FN = "/home/bmander/data/boston.gdb"
osmdb = OSMDB( OSMDB_FN )
graphdb = GraphDatabase( GRAPHDB_FN )

class BoundsHandler(tornado.web.RequestHandler):
    def get(self):
        self.write( {'bounds':osmdb.bounds()} )

class NearbyHandler(tornado.web.RequestHandler):
    def get(self):
	lat = float(self.get_argument("lat"))
        lon = float(self.get_argument("lon"))

        node_id,node_lat,node_lon,node_dist = osmdb.nearest_node(lat,lon)

        self.write( {'id':node_id,'lat':node_lat,'lon':node_lon,'dist':node_dist} )

application = tornado.web.Application([
    (r"/bounds", BoundsHandler),
    (r"/nearby", NearbyHandler),
])

if __name__ == "__main__":
    application.listen(80)
    tornado.ioloop.IOLoop.instance().start()
