/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.io.Serializable;

/**
 *
 * @author lucia
 */
public class bloco implements Serializable {

    private int[] origensCont;
    private int[] destinosCont;

    private short[][] origensBatch;
    private short[][] destinosBatch;

    private double latSup=-1;
    private double latInf;
    private double lonSup;
    private double lonInf;
    private final short indexLat;
    private final short indexLon;

    private short[] escolhaCluster;
    private short clusterPred = -1;
    private boolean alertaSemBatchs = false;

    public void acumularEscolhaCluster(int nclust, int clust) {

        if (escolhaCluster == null) {
            escolhaCluster = new short[nclust];
            for (int x = 0; x < nclust; x++) {
                escolhaCluster[x] = 0;
            }
        }
        escolhaCluster[clust]++;
    }

    public int getClusterPref() {
        return clusterPred;
    }

    public int definirClusterPredominante() {

        if (escolhaCluster == null) {
            return -1;
        }

        int max = escolhaCluster[0];
        clusterPred = 0;

        for (int c = 1; c < escolhaCluster.length; c++) {
            if (escolhaCluster[c] > max) {
                max = escolhaCluster[c];
                clusterPred = (short) c;
            }
        }

        return clusterPred;

    }

    public String relatBloco(boolean completo) {

        if (!completo) {
            return "index[" + indexLat + " " + indexLon + "]";
        } else {
            return "index[" + indexLat + " " + indexLon + "] lat[" + latInf + " " + latSup + "]; lon[" + lonInf + " " + lonSup + "]";
        }
    }

    public boolean pontoPertenceAoBloco(double lat, double lon) {

        if(latSup==-1)
            System.out.println("ERR: Bloco latSupp==-1");
        
        return (((lat >= latInf && lat <= latSup) || (lat <= latInf && lat >= latSup)) && ((lon >= lonInf && lon <= lonSup) || (lon <= lonInf && lon >= lonSup)));

    }

    public bloco(double lat1, double lat2, double lon1, double lon2, int indexLat, int indexLon, int discretTemporal, int batches) {

        latSup = lat1;
        latInf = lat2;
        lonSup = lon1;
        lonInf = lon2;

        origensCont = new int[discretTemporal];
        destinosCont = new int[discretTemporal];
        origensBatch = new short[discretTemporal][batches];
        destinosBatch = new short[discretTemporal][batches];
        for (int t = 0; t < discretTemporal; t++) {

            origensCont[t] = 0;
            destinosCont[t] = 0;
            for (int x = 0; x < batches; x++) {

                origensBatch[t][x] = 0;
                destinosBatch[t][x] = 0;
            }
        }
        this.indexLat = (short) indexLat;
        this.indexLon = (short) indexLon;
    }

    public void resetContagensBloco(int discretTemporal, int batches) {

        origensCont = new int[discretTemporal];
        destinosCont = new int[discretTemporal];
        origensBatch = new short[discretTemporal][batches];
        destinosBatch = new short[discretTemporal][batches];
        for (int t = 0; t < discretTemporal; t++) {
            origensCont[t] = 0;
            destinosCont[t] = 0;
            for (int x = 0; x < batches; x++) {
                origensBatch[t][x] = 0;
                destinosBatch[t][x] = 0;
            }
        }

        alertaSemBatchs = false;
    }

    /**
     * @return the origensCont
     */
    public int getOrigensCont(int t) {
        if (t > 0) {
            return origensCont[t];
        }
        int s = 0;
        for (int a = 0; a < origensCont.length; a++) {
            s += origensCont[a];
        }
        return s;
    }

    public void origensContMais(int t, int bat) {
        origensCont[t]++;
        if (bat != -1) {
            origensBatch[t][bat]++;
        } else {
            setAlertaSemBatchs(true);
        }
    }

    public void destinosContMais(int t, int bat) {
        destinosCont[t]++;
        if (bat != -1) {
            destinosBatch[t][bat]++;
        } else {
            setAlertaSemBatchs(true);
        }
    }

    public int getDestinosContBatch(int t, int b) {
        if (isAlertaSemBatchs()) {
            System.out.println("ERROR: AlertaSemBatchs. Contagens feitas a partir do arquivo de Cluster. Necessário refazer MatrizOD pelo método de batches.");
        }

        return destinosBatch[t][b];
    }

    public int getOrigensContBatch(int t, int b) {
        if (isAlertaSemBatchs()) {
            System.out.println("ERROR: AlertaSemBatchs. Contagens feitas a partir do arquivo de Cluster. Necessário refazer MatrizOD pelo método de batches.");
        }

        return origensBatch[t][b];
    }

    /**
     * @param origensCont the origensCont to set
     */
    public void setOrigensCont(int t, int origensCont) {
        this.origensCont[t] = origensCont;
    }

    /**
     * @return the destinosCont
     */
    public int getDestinosCont(int t) {
        if (t > 0) {
            return destinosCont[t];
        }
        int s = 0;
        for (int a = 0; a < destinosCont.length; a++) {
            s += destinosCont[a];
        }
        return s;
    }

    /**
     * @param destinosCont the destinosCont to set
     */
    public void setDestinosCont(int t, int destinosCont) {
        this.destinosCont[t] = destinosCont;
    }

    /**
     * @return the latSup
     */
    public double getLatSup() {
        return latSup;
    }

    /**
     * @return the latInf
     */
    public double getLatInf() {
        return latInf;
    }

    /**
     * @return the lonSup
     */
    public double getLonSup() {
        return lonSup;
    }

    /**
     * @return the lonInf
     */
    public double getLonInf() {
        return lonInf;
    }

    /**
     * @return the indexLat
     */
    public int getIndexLat() {
        return indexLat;
    }

    /**
     * @return the indexLon
     */
    public int getIndexLon() {
        return indexLon;
    }

    /**
     * @return the alertaSemBatchs
     */
    public boolean isAlertaSemBatchs() {
        return alertaSemBatchs;
    }

    /**
     * @param alertaSemBatchs the alertaSemBatchs to set
     */
    public void setAlertaSemBatchs(boolean alertaSemBatchs) {
        this.alertaSemBatchs = alertaSemBatchs;
    }

}
