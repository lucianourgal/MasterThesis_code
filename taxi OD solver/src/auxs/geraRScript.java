/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import taxi.od.solver.Mapping;

/**
 *
 * @author lucia
 */
public class geraRScript {

    private String filename = "Map script.r";

    public void setNomeArquivo(String n) {

        filename = n;

    }

    public void criarArquivoRggmapFromClusters(ArrayList<cluster> clusters) {

        ArrayList<Double> lat = new ArrayList<>();
        ArrayList<Double> lon = new ArrayList<>();
        ArrayList<Integer> tips = new ArrayList<>();

        int cont = 0;

        for (int c = 0; c < clusters.size(); c++) {

            cont = 0;

            for (int n = 0; n < clusters.get(c).getNosBordaFrom().size(); n++) {
                if (!((lat.indexOf(clusters.get(c).getNosBordaLat().get(n)) == lon.indexOf(clusters.get(c).getNosBordaLon().get(n))) && lon.indexOf(clusters.get(c).getNosBordaLon().get(n)) !=-1)) { //evita posições repetirdas
                    lat.add(clusters.get(c).getNosBordaLat().get(n));
                } else {
                    cont++;
                }
            }

            for (int n = 0; n < clusters.get(c).getNosBordaFrom().size(); n++) {
                if (!((lat.indexOf(clusters.get(c).getNosBordaLat().get(n)) == lon.indexOf(clusters.get(c).getNosBordaLon().get(n))) && lon.indexOf(clusters.get(c).getNosBordaLon().get(n)) !=-1)) { //evita posições repetirdas
                    lon.add(clusters.get(c).getNosBordaLon().get(n));
                } else {
                  //  cont++;
                }
            }

            //if(!(lat.contains(clusters.get(c).getNosBordaLat().get(n)) && lon.contains(clusters.get(c).getNosBordaLon().get(n))))
            tips.add(clusters.get(c).getNosBordaFrom().size() - cont);

        }

        criarArquivo(lat, lon, tips);

    }

    public void criarArquivo(ArrayList<Double> latx, ArrayList<Double> lonx, ArrayList<Integer> tipos) {

        criarArquivoX(latx, lonx, gerarCores(tipos));

    }

    /*
    # loading the required packages
library(ggplot2)
library(ggmap)

# creating a sample data.frame with your lat/lon points
lon <- c(-8.5856641,-8.5856641,-8.5856641)
lat <- c(41.1485752,41.1485752,41.1485752)
df <- as.data.frame(cbind(lon,lat))

# getting the map
mapgilbert <- get_map(location = c(lon = mean(df$lon), lat = mean(df$lat)), zoom = 13,
                      maptype = "satellite", scale = 2)

# plotting the map with some points on it
ggmap(mapgilbert) +
  geom_point(data = df, aes(x = lon, y = lat, fill = "red", alpha = 0.8), size = 2, shape = 21) +
  guides(fill=FALSE, alpha=FALSE, size=FALSE)
     */
    private void criarArquivoX(ArrayList<Double> lat, ArrayList<Double> lon, ArrayList<String> coldColours) {
        
        
        this.criarArquivoTXTParaSite(lat, lon);

        if (lat.size() < 1) {
            System.out.println("ERROR: Não pode gerar " + filename + " por vetor Lat e Lon vazios.");
            return;
        }

        for (int x = 0; x < lat.size() - 1; x++) {
            if (Objects.equals(lat.get(x), lat.get(x + 1)) && Objects.equals(lon.get(x), lon.get(x + 1))) {
                System.out.println("ALERT: Localização " + x + " igual a localização " + (x + 1) + " (criarArquivoX)");
            }
        }

        String lats = "(";
        String lons = "(";
        String color = "(";

        for (int x = 0; x < lat.size() - 1; x++) {
            lats = lats + lat.get(x) + ",";
        }
        lats = lats + lat.get(lat.size() - 1) + ")";

        for (int x = 0; x < lat.size() - 1; x++) {
            lons = lons + lon.get(x) + ",";
        }
        lons = lons + lon.get(lat.size() - 1) + ")";

        for (int x = 0; x < lat.size() - 1; x++) {
            color = color + "'" + coldColours.get(x) + "',";
        }
        color = color + "'" + coldColours.get(lat.size() - 1) + "')";

        String cod = "library(RgoogleMaps)\n"
                + "library(colorRamps)\n"
                + "library(tm)\n"
                + "library(chron)\n\n"
                + "lat = c" + lats + ";\n"
                + "lon = c" + lons + ";\n"
                + "center = c(mean(lat), mean(lon));\n"
                + "zoom <- min(MaxZoom(range(lat), range(lon)));\n"
                + "    \n"
                + "Map <- GetMap(center=center, zoom=13,markers = paste0(\"\"), destfile = \"PortoMap.png\");\n"
                + "\n"
                + "tmp <- PlotOnStaticMap(Map, lat = c" + lats + ", \n"
                + "                       lon = c" + lons + ", \n"
                + "                       destfile = \"PortoMap.png\", cex=1.5,pch=20,                       \n"
                + "                       col=c" + color + ", add=FALSE);\n"
                + "\n"
                + "# Now let's add points with the points method:\n"
                + "PlotOnStaticMap(Map, lat = c" + lats + ", \n"
                + "                lon = c" + lons + ", \n"
                + "                lwd=1.5,col=c" + color + ",  points(x = " + (lat.get(0) + 0.000001) + ", y = NULL ), add=TRUE)";

        salvarTxt(filename, cod);

        
    }

    /* ORIGINAL    
library(RgoogleMaps)
library(colorRamps)
library(tm)
library(chron)

lat = c(40.702147,40.718217,40.711614);
lon = c(-74.012318,-74.015794,-73.998284);
center = c(mean(lat), mean(lon));
zoom <- min(MaxZoom(range(lat), range(lon)));
    
Map <- GetMap(center=center, zoom=zoom,markers = paste0(""), destfile = "MyTile1.png");

tmp <- PlotOnStaticMap(Map, lat = c(40.702147,40.711614,40.718217), 
                       lon = c(-74.015794,-74.012318,-73.998284), 
                       destfile = "MyTile1.png", cex=1.5,pch=20,                       
                       col=c('red', 'blue', 'green'), add=FALSE);

# Now let's add points with the points method:
PlotOnStaticMap(Map, lat = c(40.702147,40.711614,40.718217), 
                lon = c(-74.015794,-74.012318,-73.998284), 
                lwd=1.5,col=c('red', 'blue', 'green'),  points(x = 40.702148, y = NULL ), add=TRUE)
    
     */
    //http://www.darrinward.com/lat-long/?id=2569876
    public void criarArquivoTXTParaSite(ArrayList<Double> lat, ArrayList<Double> lon) {

        if (lat.size() < 1) {
            System.out.println("ERROR: Não pode gerar " + filename + ".txt por vetor Lat e Lon vazios.");
            return;
        }
        
        if(lat.size()!= lon.size()){
            System.out.println("ERROR: lat.size("+lat.size()+") != lon.size("+lon.size()+")!!??!?");
            
        }

        String cod = "http://www.darrinward.com/lat-long/\n\n";

        for (int x = 0; x < lat.size(); x++) {
            cod = cod + lat.get(x) + ", " + lon.get(x) + "\n";
        }

        salvarTxt(filename + ".txt", cod);

    }

    public boolean salvarTxt(String name, String conteudo) {

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(name);
            fileWriter.append(conteudo);
        } catch (Exception e) {
            System.out.println("ERROR: FileWriter de '" + name + ".");
            e.printStackTrace();
            return false;
        } finally {

            try {
                fileWriter.flush();
                fileWriter.close();
                System.out.println("OK: Salvou " + name + "!");
                return true;
            } catch (IOException e) {
                System.out.println("ERROR: While flushing/closing fileWriter de '" + name + "'.");
                e.printStackTrace();
                return false;
            }
        }

    }

    public ArrayList<String> gerarCores(ArrayList<Integer> qtd) {

        ArrayList<String> cs = new ArrayList<>();

        for (int q = 0; q < qtd.size(); q++) {
            for (int x = 0; x < qtd.get(q); x++) {

                switch (q) {
                    case 0:
                        cs.add("red");
                        break;
                    case 1:
                        cs.add("green");
                        break;
                    case 2:
                        cs.add("blue");
                        break;
                    case 3:
                        cs.add("purple");
                        break;
                    case 4:
                        cs.add("orange");
                        break;
                    case 5:
                        cs.add("yellow");
                        break;
                    case 6:
                        cs.add("black");
                        break;
                    case 7:
                        cs.add("cyan");
                        break;
                    case 8:
                        cs.add("pear");
                        break;
                    case 9:
                        cs.add("pink");
                        break;
                    case 10:
                        cs.add("jade");
                        break;
                    case 11:
                        cs.add("lilac");
                        break;
                    case 12:
                        cs.add("limerick");
                        break;
                    case 13:
                        cs.add("livid");
                        break;
                    case 14:
                        cs.add("lust");
                        break;
                    case 15:
                        cs.add("mint");
                        break;
                    case 16:
                        cs.add("ming");
                        break;
                    case 17:
                        cs.add("mustard");
                        break;
                    case 18:
                        cs.add("kiwi");
                        break;
                    default:
                        cs.add("milk");
                        break;
                }

            }
        }

        return cs;

    }

    public void gerarFromIndexes(int[] mov, int[] mov2, Mapping map) {
        //criarArquivo(ArrayList<Double> lat, ArrayList<Double> lon, ArrayList<Integer> tipos)

        ArrayList<Double> latx = new ArrayList<>();
        ArrayList<Double> lonx = new ArrayList<>();
        ArrayList<Integer> tipos = new ArrayList<>();

        for (int a = 0; a < mov.length; a++) {
            //    System.out.println("Getting lat[mov["+a+"]] = lat["+mov[a]+"] = "+map.getNode_lat()[mov[a]]);
            latx.add((map.getNode_lat()[mov[a]]+map.getNode_lat()[mov2[a]])/2);
            lonx.add((map.getNode_lon()[mov[a]]+map.getNode_lon()[mov2[a]])/2);
        }

        for (int x = 0; x < latx.size() - 1; x++) {
            if (Objects.equals(latx.get(x), latx.get(x + 1)) && Objects.equals(lonx.get(x), lonx.get(x + 1))) {
                System.out.println("ALERT: Localização " + x + " igual a localização " + (x + 1) + " (gerarFromIndexes)");
            }
        }

        tipos.add(mov.length);

        criarArquivo(latx, lonx, tipos);

    }

}
