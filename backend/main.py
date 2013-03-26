import tornado.ioloop
import tornado.web

from graphserver.ext.osm.osmdb import OSMDB
from graphserver.graphdb import GraphDatabase
from graphserver.core import State, WalkOptions
import time
import graphserver

OSMDB_FN = "/home/bmander/data/boston.osmdb"
GRAPHDB_FN = "/home/bmander/data/boston.gdb"
osmdb = OSMDB( OSMDB_FN )
graphdb = GraphDatabase( GRAPHDB_FN )
graph = graphdb.incarnate()

class BoundsHandler(tornado.web.RequestHandler):
    def get(self):
        self.write( {'bounds':osmdb.bounds()} )

class NearbyHandler(tornado.web.RequestHandler):
    def get(self):
	lat = float(self.get_argument("lat"))
        lon = float(self.get_argument("lon"))

        node_id,node_lat,node_lon,node_dist = osmdb.nearest_node(lat,lon)

        self.write( {'id':node_id,'lat':node_lat,'lon':node_lon,'dist':node_dist} )

class PathHandler(tornado.web.RequestHandler):
    def get(self):
	lat1 = float( self.get_argument("lat1") )
        lon1 = float( self.get_argument("lon1") )
	lat2 = float( self.get_argument("lat2") )
        lon2 = float( self.get_argument("lon2") )

        osm_node_id1,node_lat,node_lon,node_dist = osmdb.nearest_node(lat1,lon1)
        osm_node_id2,node_lat,node_lon,node_dist = osmdb.nearest_node(lat2,lon2)

        if osm_node_id1 is None:
            self.write( {'error':'origin lookup error'} )
            return
        if osm_node_id2 is None:
            self.write( {'error':'destination lookup error'} )
            return	

        node_id1 = "osm-"+osm_node_id1
        node_id2 = "osm-"+osm_node_id2

        t0 = int(time.time())
        wo = WalkOptions()
        spt = graph.shortest_path_tree( node_id1, node_id2, State(1,t0), wo )

        vertices,edges = spt.path( node_id2 )

        geoms = []
        for edge in edges:
            if edge.payload.__class__ == graphserver.core.Street:
                wayid,parentid,from_nd,to_nd,dist,geom,tags = osmdb.edge( edge.payload.name )
                geoms.append( geom )

        self.write( {'id1':node_id1, 'id2':node_id2, 'geom':geoms} )

application = tornado.web.Application([
    (r"/bounds", BoundsHandler),
    (r"/nearby", NearbyHandler),
    (r"/path", PathHandler),
    (r"/(.*)", tornado.web.StaticFileHandler, {"path":"./static"}) 
])

if __name__ == "__main__":
    application.listen(80)
    tornado.ioloop.IOLoop.instance().start()
