# fetch citibike information and dump to csv
from urllib import urlopen
import simplejson as json

URL = "http://citibikenyc.com/stations/json/"

fp = urlopen( URL )
data = json.loads( fp.read() )

fpout = open( "citibike.csv", "w" )
fpout.write( "id,lat,lon,name,docks\n" )
for station in data["stationBeanList"]:
	id = station["id"]
	lat = station["latitude"]
	lon = station["longitude"]
	name = station["stationName"]
	docks = station["totalDocks"]

	fpout.write( "%s,%s,%s,\"%s\",%s\n"%(id,lat,lon,name.encode("utf-8"),docks) )

print "done"