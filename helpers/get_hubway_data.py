from urllib import urlopen
from xml.dom.minidom import parseString

fp = urlopen( "http://thehubway.com/data/stations/bikeStations.xml" )

dom = parseString( fp.read() )

fpout = open( "hubway.csv", "w" )
fpout.write( "id,name,latitude,longitude\n")

for station in dom.getElementsByTagName("station"):
    id = station.getElementsByTagName("id")[0].firstChild.data
    name = station.getElementsByTagName("name")[0].firstChild.data
    lat = station.getElementsByTagName("lat")[0].firstChild.data
    lon = station.getElementsByTagName("long")[0].firstChild.data

    fpout.write("%s,%s,%s,%s\n"%(id,name,lat,lon))