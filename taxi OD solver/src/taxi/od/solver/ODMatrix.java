/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package taxi.od.solver;

import auxs.bloco;
import auxs.cluster;
import auxs.geradordeRede2;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * @author Luciano
 */
public class ODMatrix implements Serializable {

    private final short ROIsizeM;
    private double ROIsizeDeg = -1;

    private int ROInumberLat = -1;
    private int ROInumberLon = -1;
    private short discretTemporal;
    private final double minInterv;

    private double menorLat = -1;
    private double menorLon;

    // private int[][][] ODmatrix;
    private byte[][] clusterDoBloco;
    private byte[][] clusterDoBlocoBackup;
    private int[][][] contagemDoBlocoOrig;
    private int[][][] contagemDoBlocoDest;

    private double[][][] ODmatrixClusters;
    private double[][][][] ODmatrixClustersBatch;
    private double[][][] ODmatrixClustersVARIANCE;

    private ArrayList<Integer>[][][] ODparFromNode;
    private ArrayList<Integer>[][][] ODparToNode;
    private ArrayList<Integer>[][][] ODparIndArestaGeral;
    private ArrayList<Double>[][][] ODparArestaCont;
    private int[][][] ODparPassagens;
    private int[][][] ODparPassagensBACKUP;

    private short numeroClusters;

    ArrayList<cluster> clusters;
    ArrayList<bloco> blocos;
    ArrayList<bloco> blocosBackup;

    ArrayList<cluster> melhoresClusters;
    private double menorCusto;

    private int contBlocos;

    private ArrayList<Integer> quadsLatPart;
    private ArrayList<Integer> quadsLonPart;
    private ArrayList<Integer> quadsLatCheg;
    private ArrayList<Integer> quadsLonCheg;
    private ArrayList<Integer> batchDaViagem;
    private ArrayList<Integer> tempoDaViagem;

    final DecimalFormat df5 = new DecimalFormat(".#####");
    DecimalFormat df2 = new DecimalFormat(".##");

    private final int minimoContParaMostrarLinhaColunaBlocos;
    private short indexBordaSup = 0;
    private short indexBordaInf;
    private short indexBordaEsq = 0;
    private short indexBordaDir;
    private int batchSize;

    public void setMenorLatLon(double lat, double lon) {
        menorLat = lat;
        menorLon = lon;
    }

    public double getODMatrixClusterVariance(int o, int d, int t) {
        if (ODmatrixClustersVARIANCE[o][d][t] > 0.15) {
            return ODmatrixClustersVARIANCE[o][d][t];
        } else {
            return 0.12;
        }
    }

    public void setODMatrixHardCode(ArrayList<Double> odm, int size) {

        System.out.println("setODMatrixHardCode(odm.size=" + odm.size() + ", " + size + ")");
        discretTemporal = 1;
        batchSize = 1;

        numeroClusters = (short) size;
        ODmatrixClusters = new double[size][size][discretTemporal];
        ODmatrixClustersBatch = new double[size][size][discretTemporal][batchSize];
        ODmatrixClustersVARIANCE = new double[size][size][discretTemporal];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                ODmatrixClusters[i][j][0] = odm.get(i * size + j);
                ODmatrixClustersBatch[i][j][0][0] = odm.get(i * size + j);

            }
        }

        //"gambi"
        clusters = new ArrayList<>();
        for (int a = 0; a < numeroClusters; a++) {
            clusters.add(new cluster());                       //criando clusters novos
        }

    }

    public ODMatrix(int RoiSizeM, int roiNmbLat, int roiNumbLon, int discretizacaoTemporal,
            int numClust, int minimoContParaMostrarLinhaColunaBlocos, double mlat, double mlon, int batchSize) {

        setMenorLatLon(mlat, mlon);
        this.batchSize = batchSize;

        this.minimoContParaMostrarLinhaColunaBlocos = minimoContParaMostrarLinhaColunaBlocos;
        indexBordaInf = (short) roiNmbLat;
        indexBordaDir = (short) roiNumbLon;

        quadsLatPart = new ArrayList<>();
        quadsLonPart = new ArrayList<>();
        quadsLatCheg = new ArrayList<>();
        quadsLonCheg = new ArrayList<>();
        tempoDaViagem = new ArrayList<>();
        batchDaViagem = new ArrayList<>();

        numeroClusters = (short) numClust;
        ROIsizeM = (short) RoiSizeM;
        ROIsizeDeg = ROIsizeM * 0.0000089;
        ROInumberLat = roiNmbLat;
        ROInumberLon = roiNumbLon;
        discretTemporal = (short) discretizacaoTemporal;
        minInterv = (24 * 60) / discretTemporal;

        //  ODmatrix = new int[ROInumberLat * ROInumberLon][ROInumberLat * ROInumberLon][discretTemporal];
        ODmatrixClusters = new double[numClust][numClust][discretTemporal];
        ODmatrixClustersBatch = new double[numClust][numClust][discretTemporal][batchSize];
        ODmatrixClustersVARIANCE = new double[numClust][numClust][discretTemporal];

        ODparFromNode = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparToNode = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparIndArestaGeral = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparArestaCont = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparPassagens = new int[numClust][numClust][discretizacaoTemporal + 1];

        for (int t = 0; t <= discretTemporal; t++) {
            for (int a = 0; a < numClust; a++) {
                for (int b = 0; b < numClust; b++) {
                    if (t < discretTemporal) {
                        ODmatrixClusters[a][b][t] = 0;
                        ODmatrixClustersVARIANCE[a][b][t] = -1;

                        for (int bat = 0; bat < batchSize; bat++) {
                            ODmatrixClustersBatch[a][b][t][bat] = 0;
                        }
                    }

                    ODparFromNode[a][b][t] = new ArrayList<>();
                    ODparToNode[a][b][t] = new ArrayList<>();
                    ODparIndArestaGeral[a][b][t] = new ArrayList<>();
                    ODparArestaCont[a][b][t] = new ArrayList<>();
                    ODparPassagens[a][b][t] = 0;

                }
            }
        }

        /*for (int a = 0; a < (ROInumberLon * ROInumberLat); a++) {
            for (int b = 0; b < (ROInumberLon * ROInumberLat); b++) {
                for (int t = 0; t < (discretizacaoTemporal); t++) {
        ODmatrix[a][b][t] = 0;
               }           }
        }*/
        criarBlocos();
    }

    public void definirCaminhosRedeFechada(Mapping mape, VirtualSensors v, int cls) {
        numeroClusters = (short) cls;
        int numClust = numeroClusters;
        int discretizacaoTemporal = 0;
        discretTemporal = 0;
        geradordeRede2 gerador = new geradordeRede2();

        ODparFromNode = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparToNode = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparIndArestaGeral = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparArestaCont = new ArrayList[numClust][numClust][discretizacaoTemporal + 1];
        ODparPassagens = new int[numClust][numClust][discretizacaoTemporal + 1];

        for (int t = 0; t <= discretTemporal; t++) {
            for (int a = 0; a < numClust; a++) {
                for (int b = 0; b < numClust; b++) {
                    if (t < discretTemporal) {
                        ODmatrixClustersVARIANCE[a][b][t] = -1;
                    }

                    ODparFromNode[a][b][t] = new ArrayList<>();
                    ODparToNode[a][b][t] = new ArrayList<>();
                    ODparIndArestaGeral[a][b][t] = new ArrayList<>();
                    ODparArestaCont[a][b][t] = new ArrayList<>();
                    ODparPassagens[a][b][t] = 1;
                }
            }
        }

        //definir caminhos
        /*ArrayList<taxi_Trip_Instance> cam = gerador.getCaminhosOD(mape, v);
        for (int a = 0; a < numClust; a++) {
            for (int b = 0; b < numClust; b++) {

                if (cam.get(a * numClust + b).getPosNodesEmMapa().size() > 1 && ODmatrixClustersBatch[a][b][0][0] > 0) {
                    for (int z = 0; z < cam.get(a * numClust + b).getPosNodesEmMapa().size() - 1; z++) {
                        ODparFromNode[a][b][0].add(cam.get(a * numClust + b).getPosNodesEmMapa().get(z));
                        ODparToNode[a][b][0].add(cam.get(a * numClust + b).getPosNodesEmMapa().get(z + 1));
                        ODparIndArestaGeral[a][b][0].add(v.findIndiceAresta(cam.get(a * numClust + b).getPosNodesEmMapa().get(z), cam.get(a * numClust + b).getPosNodesEmMapa().get(z + 1)));
                        ODparArestaCont[a][b][0].add(1.0);
                    }
                }
            }
        }*/
        //gerador.gerarAssignmentMatrix_BPR(mape);
        gerador.gerarAssignmentMatrix_FromVissum(mape);
        ArrayList<Integer> doParOD = gerador.getDOParOD();
        ArrayList<Integer> daAresta = gerador.getDaAresta();
        ArrayList<Double> probODaresta = gerador.getProbODAresta();
        int a,b;
        
        for(int x=0;x<doParOD.size();x++){
            b = doParOD.get(x)%numeroClusters;
            a = (doParOD.get(x)-b)/numeroClusters;
                    
            ODparFromNode[a][b][0].add(mape.getFromNodI().get(daAresta.get(x)));
            ODparToNode[a][b][0].add(mape.getToNodI().get(daAresta.get(x)));
            ODparIndArestaGeral[a][b][0].add(daAresta.get(x));
            ODparArestaCont[a][b][0].add(probODaresta.get(x));
        
        }

    }

    int [] rotasPorAresta = null;
    
    public int getContRotasPorAresta(int aresta, int tempo, int totalArestas){
    //getProbUsoArestaPorParODeIndexAresta(o, d, t1, sensorAresta[ars]))
    if(rotasPorAresta==null){
        rotasPorAresta = new int[totalArestas];
    for(int a=0;a<totalArestas;a++)
        rotasPorAresta[a] = -1;
    }else if(rotasPorAresta[aresta]>-1)
        return rotasPorAresta[aresta];
    
    int cont = 0;
    for(int o=0;o<numeroClusters;o++)
        for(int d=0;d<numeroClusters;d++)
            if(ODparIndArestaGeral[o][d][tempo].contains(aresta))
                cont++;
            
    rotasPorAresta[aresta] = cont;
    return cont;
    }
    
    
    public void redefinirODparArestaContAPartirDeVirtualSensors(VirtualSensors vt, int tempo, int batch) {

        //ArrayList ODparArestaCont[a][b], ArrayList ODparToNode[a][b], ODparFromNode[a][b]
        for (int a = 0; a < numeroClusters; a++) {
            for (int b = 0; b < numeroClusters; b++) {
                for (int ar = 0; ar < ODparToNode[a][b][tempo].size(); ar++) {
                    //  System.out.println("O="+a+"; D="+b+"; ar="+ar+"; t="+tempo+"; batch="+batch+"; ODparArestaCont[O][D].size="+ODparArestaCont[a][b].size()+"; ");
                    //  System.out.println("ODparIndArestaGeral[a][b].size="+ODparIndArestaGeral[a][b].size()+"; ODmatrixClustersBatch.length="+ODmatrixClustersBatch.length);
                    double div = ((double) ODmatrixClustersBatch[a][b][tempo][batch]);
                    //double div = ((double)ODparPassagensBACKUP[a][b]);
                    ODparArestaCont[a][b][tempo].set(ar, ((double) vt.getContArestaBatch(ODparIndArestaGeral[a][b][tempo].get(ar), tempo, batch))
                            / //    div); 
                            div);

                    if (ODparArestaCont[a][b][tempo].get(ar) < 0 || ODparArestaCont[a][b][tempo].get(ar) > 1) {
                        System.out.println("ERROR: ODparArestaCont[" + a + "][" + b + "].get(" + ar + ")=" + ODparArestaCont[a][b][tempo].get(ar) + "; (" + vt.getContArestaBatch(ODparIndArestaGeral[a][b][tempo].get(ar), tempo, batch) + "/" + div + ") ");
                    }

                }
            }
        }

        System.out.println("OK: Redefiniu probabilidades de uso de aresta por par OD de acordo com VirtualSensors e ODMatrix");
    }

    public void descobrirBordas() {
        // indexBordaSup, Inf, Esq, Dir
        //contagemDoBlocoOrig, contagemDoBlocoDest, ROInumberLat, ROInumberLon

        for (int a = 0; a < ROInumberLat; a++) {
            for (int b = 0; b < ROInumberLon; b++) {
                if (contBlocoT(a, b) >= minimoContParaMostrarLinhaColunaBlocos) { //encontrou o minimo
                    a = ROInumberLat;
                    b = ROInumberLon;
                } else {

                    //System.out.println("int = "+a+". short = "+ (short) a);
                    indexBordaSup = (short) a; //caso contrário, reserva linha para exclusão
                }
            }
        }

        for (int a = ROInumberLat - 1; a > 0; a--) {
            for (int b = 0; b < ROInumberLon; b++) {
                if (contBlocoT(a, b) >= minimoContParaMostrarLinhaColunaBlocos) { //encontrou o minimo
                    a = -1;
                    b = ROInumberLon;
                } else {
                    indexBordaInf = (short) (a + 1); //caso contrário, reserva linha para exclusão
                }
            }
        }

        //colunas
        for (int b = 0; b < ROInumberLon; b++) {
            for (int a = 0; a < ROInumberLat; a++) {
                if (contBlocoT(a, b) >= minimoContParaMostrarLinhaColunaBlocos) { //encontrou o minimo
                    a = ROInumberLat;
                    b = ROInumberLon;
                } else {
                    indexBordaEsq = (short) b; //caso contrário, reserva coluna para exclusão
                }
            }
        }
        for (int b = ROInumberLon - 1; b > 0; b--) {
            for (int a = 0; a < ROInumberLat; a++) {
                if (contBlocoT(a, b) >= minimoContParaMostrarLinhaColunaBlocos) { //encontrou o minimo
                    a = ROInumberLat;
                    b = -1;
                } else {
                    indexBordaDir = (short) (b + 1); //caso contrário, reserva coluna para exclusão
                }
            }
        }

    }

    public int contBlocoT(int x, int y) {
        int som = 0;
        for (int t = 0; t < discretTemporal; t++) {
            som += (contagemDoBlocoOrig[x][y][t] + contagemDoBlocoDest[x][y][t]);
        }
        return som;
    }

    public void criarBlocos() {

        blocos = new ArrayList<>();

        if (ROInumberLat == -1) {
            System.out.println("ERROR: ROInumberLat == -1 (ODmatrix.criarBlocos)");
        }
        if (ROIsizeDeg == -1) {
            System.out.println("ERROR: ROIsizeDeg == -1 (ODmatrix.criarBlocos)");
        }
        if (menorLat == -1) {
            System.out.println("ERROR: menorLat == -1 (ODmatrix.criarBlocos)");
        }

        //bloco(double lat1, double lat2, double lon1, double lon2, int indexLat, int indexLon)
        for (int la = 0; la < ROInumberLat; la++) {
            for (int lo = 0; lo < ROInumberLon; lo++) {
                blocos.add(new bloco(menorLat + la * ROIsizeDeg, menorLat + (la + 1) * ROIsizeDeg, menorLon + lo * ROIsizeDeg,
                        menorLon + (lo + 1) * ROIsizeDeg, la, lo, discretTemporal, batchSize));
            }
        }

        contBlocos = blocos.size();

        clusterDoBloco = new byte[ROInumberLat][ROInumberLon];
        clusterDoBlocoBackup = new byte[ROInumberLat][ROInumberLon];

        contagemDoBlocoOrig = new int[ROInumberLat][ROInumberLon][discretTemporal];
        contagemDoBlocoDest = new int[ROInumberLat][ROInumberLon][discretTemporal];

    }

    public void statsBlocos() {

        DescriptiveStatistics stats = new DescriptiveStatistics();
        //stats blocos
        for (int b = 0; b < blocos.size(); b++) {
            stats.addValue(blocos.get(b).getOrigensCont(-1));
        }

        System.out.println("STATS: Blocos (origens). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");
        stats = new DescriptiveStatistics();
        //stats blocos
        for (int b = 0; b < blocos.size(); b++) {
            stats.addValue(blocos.get(b).getDestinosCont(-1));
        }

        System.out.println("STATS: Blocos (destinos). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");
        stats = new DescriptiveStatistics();
        //stats blocos
        for (int b = 0; b < blocos.size(); b++) {
            stats.addValue(blocos.get(b).getOrigensCont(-1) + blocos.get(b).getDestinosCont(-1));
        }

        System.out.println("STATS: Blocos (origens+dest). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

    }

    public void cortarBordas() {

        System.out.println("PROC: Cortando bordas... inicio com " + blocos.size() + " blocos");

        blocosBackup = new ArrayList<>();

        for (int b = 0; b < blocos.size(); b++) {
            if (blocos.get(b).getIndexLat() >= indexBordaSup && blocos.get(b).getIndexLat() < indexBordaInf
                    && blocos.get(b).getIndexLon() >= indexBordaEsq && blocos.get(b).getIndexLon() < indexBordaDir) {
                blocosBackup.add(blocos.get(b));
            }
        }

        System.out.println("OK: Reduziu de " + blocos.size() + " para " + blocosBackup.size() + " blocos, retirando bordas.");

        blocos = new ArrayList<>();

        for (int b = 0; b < blocosBackup.size(); b++) {
            blocos.add(blocosBackup.get(b));
        }

    }

    public void criarClusters() {

        statsBlocos();

        blocosBackup = new ArrayList<>();
        int r;
        double aux;
        for (int b = 0; b < blocos.size(); b++) {
            //    blocosBackup.add(blocos.get(b)); - adicionado em cortar bordas
            for (int t = 0; t < discretTemporal; t++) {
                contagemDoBlocoOrig[blocos.get(b).getIndexLat()][blocos.get(b).getIndexLon()][t] += blocos.get(b).getOrigensCont(t);
                contagemDoBlocoDest[blocos.get(b).getIndexLat()][blocos.get(b).getIndexLon()][t] += blocos.get(b).getDestinosCont(t);
            }
        }

        descobrirBordas();

        cortarBordas();

        System.out.println("PROC: Iniciando clusters por k-means (" + contBlocos + " blocos, " + getNumeroClusters() + " clusters)... " + horaAtual());

        for (int t = 0; t < 15; t++) {

            clusters = new ArrayList<>();
            blocos = new ArrayList<>();

            for (int b = 0; b < blocosBackup.size(); b++) {
                blocos.add(blocosBackup.get(b));
            }

            for (int a = 0; a < getNumeroClusters(); a++) {
                clusters.add(new cluster());
                r = (int) (blocos.size() * Math.random());
                //  System.out.println(a+" "+r);
                clusters.get(a).addBloco(blocos.get(r));
                clusterDoBloco[blocos.get(r).getIndexLat()][blocos.get(r).getIndexLon()] = (byte) a;
                blocos.remove(r);
            }

            if (t == 0) {
                menorCusto = kmeans();
                melhoresClusters = clusters;

                for (int a = 0; a < ROInumberLat; a++) {
                    System.arraycopy(clusterDoBloco[a], 0, clusterDoBlocoBackup[a], 0, ROInumberLon);
                }

            } else {
                aux = kmeans();
                if (aux < menorCusto) {
                    // System.out.println("Custo "+aux+". Melhor que "+menorCusto+". Novos clusters.");
                    menorCusto = aux;
                    melhoresClusters = clusters;

                    for (int a1 = 0; a1 < ROInumberLat; a1++) {
                        System.arraycopy(clusterDoBloco[a1], 0, clusterDoBlocoBackup[a1], 0, ROInumberLon);
                    }

                } else {
                    //System.out.println("Custo "+aux+". Mantém "+menorCusto+";");
                }

            }

        }

        clusters = melhoresClusters;

        for (int a = 0; a < ROInumberLat; a++) {
            System.arraycopy(clusterDoBlocoBackup[a], 0, clusterDoBloco[a], 0, ROInumberLon);
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int x = 0; x < clusters.size(); x++) {
            stats.addValue(clusters.get(x).getBlocosSize());
        }

        System.out.println("STATS: Cluster (blocos). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

        System.out.println("OK: Encerrou cálculo de k-means. " + horaAtual() + "\n");

        calcMatrixClust();

    }

    public double kmeans() {// indexBordaSup, Inf, Esq, Dir

        DescriptiveStatistics stats = new DescriptiveStatistics();
        int blocosLivres = blocos.size();
        double menorC;
        double cost = 0;
        int indexMenorC, indexMenorB = -1;
        while (blocosLivres > 0) {
            indexMenorC = 0;
            menorC = 99999;
            indexMenorB = -1;
            for (int c = 0; c < clusters.size(); c++) {
                for (int b = 0; b < blocos.size(); b++) {
                    if (clusters.get(c).custoDeAdicionar(-1, blocos.get(b)) < menorC) {
                        indexMenorB = b;
                        indexMenorC = c;
                        menorC = clusters.get(c).custoDeAdicionar(-1, blocos.get(b));
                    }
                }
            }

            //encontrou menor custo
            if (indexMenorB != -1) {
                //adiciona bloco a cluster
                clusters.get(indexMenorC).addBloco(blocos.get(indexMenorB));
                //marca na matriz em que cluster o bloco está
                clusterDoBloco[blocos.get(indexMenorB).getIndexLat()][blocos.get(indexMenorB).getIndexLon()] = (byte) indexMenorC;
                //remove bloco do vetor
                blocos.remove(indexMenorB);
                cost += menorC;
                stats.addValue(menorC);
            }
            blocosLivres--;

        }
        // System.out.println("STATS: Kmeans (costs - " + menorCusto + "). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");
        return cost;
    }

    public void addViagemNaMatrix(int quadLatPart, int quadLonPart,
            int quadLatCheg, int quadLonCheg, Timestamp tempo, int bat) {

        int min = tempo.getHours() * 60 + tempo.getMinutes();
        int t = (int) (min / minInterv);

        if (quadLatPart == -1 || quadLonPart == -1 || quadLatCheg == -1 || quadLonCheg == -1) {
            // ODmatrix[quadLatPart + (quadLonPart * ROInumberLat)][quadLatCheg + (quadLonCheg * ROInumberLat)][t]++;
            return;
        }
        acresBlocoO(quadLatPart, quadLonPart, t, bat);
        acresBlocoD(quadLatCheg, quadLonCheg, t, bat);

        quadsLatPart.add(quadLatPart);
        quadsLonPart.add(quadLonPart);
        quadsLatCheg.add(quadLatCheg);
        quadsLonCheg.add(quadLonCheg);
        tempoDaViagem.add(t);
        batchDaViagem.add(bat);

    }

    private int pc(int q, int l) {
        return pertenceAoCluster(q, l);
    }

    public int pertenceAoCluster(int qlat, int qlon) {

        int r = -1;

        for (int c = 0; c < clusters.size(); c++) {
            if (clusters.get(c).temBloco(qlat, qlon)) {
                r = c;
            }
        }

        /*  if(r>=numeroClusters){
            System.out.println("????: Cluster n."+r+" de "+numeroClusters+"("+clusters.size()+"). (ODMatrix.pertenceAoCluster("+qlat+", "+qlon+"))");
            r = -1;
        }*/
        return r;
    }

    public double getODMatrixClustersBatch(int o, int d, int t, int bat) {
        //try{
        return ODmatrixClustersBatch[o][d][t][bat];
        /* }catch(Exception e){
        System.out.println("ERROR: ODmatrixClustersBatch["+o+"]["+d+"]["+t+"]["+bat+"]");
        return -1;
        }*/
    }

    public void calcVarianciaODMatrix() {
        for (int t = 0; t < discretTemporal; t++) {
            for (int c1 = 0; c1 < clusters.size(); c1++) {
                for (int c2 = 0; c2 < clusters.size(); c2++) {
                    //stats.addValue(ODmatrixClusters[c1][c2][t]);
                    ODmatrixClustersVARIANCE[c1][c2][t] = variancia(ODmatrixClustersBatch[c1][c2][t]);
                }
            }
        }

    }

    public void calcMatrixClust() {
        int a1, b;
        int somCD = 0;
        int somCI = 0;
        int fora = 0;

        if (quadsLatPart.size() < 1) {
            System.out.println("ERROR: Não há registros de viagens para recuperar! (ODmatrix.calcMatrixClust)");
        } else {
            System.out.println("PROC: Calculando matriz O-D a partir de " + quadsLatPart.size() + " registros de viagens...");
            ODmatrixClusters = new double[numeroClusters][numeroClusters][discretTemporal];
            ODmatrixClustersBatch = new double[numeroClusters][numeroClusters][discretTemporal][batchSize];

            DescriptiveStatistics stats = new DescriptiveStatistics();

            for (int a = 0; a < quadsLatPart.size(); a++) {
                a1 = pc(quadsLatPart.get(a), quadsLonPart.get(a)); //Partida percentece ao cluster de indice A1
                b = pc(quadsLatCheg.get(a), quadsLonCheg.get(a));  //Chegada percentece ao cluster de indice B
                if (a1 > -1 && b > -1) {

                    ODmatrixClusters[a1][b][tempoDaViagem.get(a)]++;
                    ODmatrixClustersBatch[a1][b][tempoDaViagem.get(a)][batchDaViagem.get(a)]++;

                    if (a1 == b) { //cluster partida = cluster chegada
                        somCI++;
                    } else {
                        somCD++;
                    }

                } else {
                    fora++;
                }
            }

            calcVarianciaODMatrix();

            int clusterVazio = 0;
            boolean vz;

            boolean[] cheio = new boolean[clusters.size()];
            for (int x = 0; x < clusters.size(); x++) {
                cheio[x] = false;
            }

            for (int t = 0; t < discretTemporal; t++) {
                for (int c1 = 0; c1 < clusters.size(); c1++) { //para todos os clusters
                    vz = true;

                    for (int c2 = 0; c2 < clusters.size(); c2++) { //entre outros clusters

                        if (ODmatrixClusters[c1][c2][t] != 0 || ODmatrixClusters[c2][c1][t] != 0) //se vai ou vem entre eles, está ocupado.
                        {
                            vz = false; //um caso de utilização
                            c2 = clusters.size();
                        }

                    }

                    if (vz == false) {  //se foi usado pelo menos uma vez (independente de tempo), o cluster não é vazio
                        cheio[c1] = true;
                    }

                }
            }
            int totalV = 0;
            for (int x = 0; x < clusters.size(); x++) {
                if (!cheio[x]) {
                    clusterVazio++;
                }
                for (int t = 0; t < discretTemporal; t++) {
                    for (int x2 = 0; x2 < clusters.size(); x2++) {
                        totalV += ODmatrixClusters[x][x2][t];
                    }
                }
            }

            System.out.println("INFO: " + 100 * (((double) somCI) / (double) (somCD + somCI)) + " % (" + somCI + "/" + totalV + ") das viagens começam e terminam no mesmo cluster! Desvio padrão: " + stats.getStandardDeviation() + "; " + fora + " Viagens sem para O-D identificado! Clusters vazios: " + clusterVazio + "/" + clusters.size());

        }

    }

    public void acresBlocoO(int quadLat, int quadLon, int t, int bat) {

        for (int b = 0; b < blocos.size(); b++) {
            if (blocos.get(b).getIndexLat() == quadLat && blocos.get(b).getIndexLon() == quadLon) {
                blocos.get(b).origensContMais(t, bat);
            }
        }

    }

    public void acresBlocoD(int quadLat, int quadLon, int t, int bat) {

        for (int b = 0; b < blocos.size(); b++) {
            if (blocos.get(b).getIndexLat() == quadLat && blocos.get(b).getIndexLon() == quadLon) {
                blocos.get(b).destinosContMais(t, bat);
            }
        }

    }

    public boolean salvarDat(String name) {

        try {
            FileOutputStream arquivoGrav = new FileOutputStream(name + ".dat");
            ObjectOutputStream objGravar = new ObjectOutputStream(arquivoGrav);
            objGravar.writeObject(this);
            objGravar.flush();
            objGravar.close();
            arquivoGrav.flush();
            arquivoGrav.close();
            System.out.println("OK: ODMatrix '" + name + "' salva em .dat!");
            return true;
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("ERROR: Falha ao salvar .dat de " + name);
            return false;
        }

    }

    public boolean salvarCsv(String name, boolean todosDetalhesBloco) {

        //Delimiter used in CSV file
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        //CSV file header
        String FILE_HEADER = "clust,";
        for (int a = 0; a < (clusters.size()); a++) {
            FILE_HEADER = FILE_HEADER + "c" + a + ", ";
        }

        FileWriter fileWriter = null;

        try {

            int t = 0;

            fileWriter = new FileWriter(name + ".csv");
            //Write the CSV file header
            fileWriter.append(FILE_HEADER);
            //Add a new line separator after the header
            fileWriter.append(NEW_LINE_SEPARATOR);
            //Write a new student object list to the CSV file

            //  for (; t < discretTemporal; t++) {
            for (int a = 0; a < (clusters.size()); a++) {

                fileWriter.append("c " + a);
                fileWriter.append(COMMA_DELIMITER);

                for (int b = 0; b < (clusters.size()); b++) {

                    fileWriter.append(String.valueOf(ODmatrixClusters[a][b]));
                    fileWriter.append(COMMA_DELIMITER);

                }

                fileWriter.append(NEW_LINE_SEPARATOR);
            }

            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);

            // } //fim laço de tempo
            //salvar características dos clusters
            fileWriter.append("Clust");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Blocos");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Cont Orig");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Cont Dest");
            fileWriter.append(COMMA_DELIMITER);

            fileWriter.append("Lat Min");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lat Max");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lon Min");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lon Max");
            fileWriter.append(COMMA_DELIMITER);

            fileWriter.append("Detalhes bls");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (int a = 0; a < (clusters.size()); a++) {

                fileWriter.append("c" + a);
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(clusters.get(a).getBlocosSize() + " bs");
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(clusters.get(a).getOrigensCont(-1)));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(clusters.get(a).getDestinosCont(-1)));
                fileWriter.append(COMMA_DELIMITER);

                fileWriter.append("[" + clusters.get(a).getLatInf());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(clusters.get(a).getIndexLatSup() + "]");
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append("[" + clusters.get(a).getLonInf());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(clusters.get(a).getIndexLonSup() + "]");
                fileWriter.append(COMMA_DELIMITER);

                for (int b = 0; b < clusters.get(a).getBlocosSize(); b++) {
                    fileWriter.append(clusters.get(a).getBlocos().get(b).relatBloco(todosDetalhesBloco));
                    fileWriter.append(COMMA_DELIMITER);
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
            }

            //imprimir demonstrativo de cluster (// indexBordaSup, Inf, Esq, Dir)
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("Clusters");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("dos");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("blocos");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lat[" + indexBordaSup + " " + indexBordaInf + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lon[" + indexBordaEsq + " " + indexBordaDir + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BlocoSizeM =");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(ROIsizeM + "");
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (int a = indexBordaSup; a < indexBordaInf; a++) {
                for (int b = indexBordaEsq; b < indexBordaDir; b++) {
                    fileWriter.append(clusterDoBloco[a][b] + "");
                    fileWriter.append(COMMA_DELIMITER);
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
            }

            //imprimir contagens origem
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("Contagens");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Orig");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("blocos");
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lat[" + indexBordaSup + " " + indexBordaInf + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lon[" + indexBordaEsq + " " + indexBordaDir + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BlocoSizeM =");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(ROIsizeM + "");
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (int a = indexBordaSup; a < indexBordaInf; a++) {
                for (int b = indexBordaEsq; b < indexBordaDir; b++) {

                    int c2 = 0;
                    for (int t2 = 0; t2 < discretTemporal; t2++) {
                        c2 += contagemDoBlocoOrig[a][b][t2];
                    }

                    fileWriter.append(c2 + "");
                    fileWriter.append(COMMA_DELIMITER);
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
            }

            //imprimir contagens origem
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("Contagens");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Dest");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("blocos");
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lat[" + indexBordaSup + " " + indexBordaInf + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lon[" + indexBordaEsq + " " + indexBordaDir + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BlocoSizeM =");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(ROIsizeM + "");
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (int a = indexBordaSup; a < indexBordaInf; a++) {
                for (int b = indexBordaEsq; b < indexBordaDir; b++) {
                    int ca = 0;
                    for (int t2 = 0; t2 < discretTemporal; t2++) {
                        ca += contagemDoBlocoDest[a][b][t2];
                    }

                    fileWriter.append(ca + "");
                    fileWriter.append(COMMA_DELIMITER);
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
            }

            //imprimir contagem geral blocos
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("Contagens");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("total");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("blocos");
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lat[" + indexBordaSup + " " + indexBordaInf + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BORDAS.");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lon[" + indexBordaEsq + " " + indexBordaDir + "]");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("BlocoSizeM =");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(ROIsizeM + "");
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (int a = indexBordaSup; a < indexBordaInf; a++) {
                for (int b = indexBordaEsq; b < indexBordaDir; b++) {
                    int dest = 0, orig = 0;
                    for (int t2 = 0; t2 < discretTemporal; t2++) {
                        dest += contagemDoBlocoDest[a][b][t2];
                        orig += contagemDoBlocoOrig[a][b][t2];
                    }

                    fileWriter.append((dest + orig) + "");
                    fileWriter.append(COMMA_DELIMITER);
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
            }

            //salvar detalhes de blocos
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("Cluster");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Bloco");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Cont Orig");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Cont Dest");
            fileWriter.append(COMMA_DELIMITER);

            fileWriter.append(NEW_LINE_SEPARATOR);

            for (int a = 0; a < (clusters.size()); a++) {
                for (int b = 0; b < clusters.get(a).getBlocosSize(); b++) {
                    fileWriter.append("c " + a);
                    fileWriter.append(COMMA_DELIMITER);

                    fileWriter.append(clusters.get(a).getBlocos().get(b).relatBloco(todosDetalhesBloco));
                    fileWriter.append(COMMA_DELIMITER);

                    fileWriter.append(clusters.get(a).getBlocos().get(b).getOrigensCont(-1) + "");
                    fileWriter.append(COMMA_DELIMITER);

                    fileWriter.append(clusters.get(a).getBlocos().get(b).getDestinosCont(-1) + "");
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(NEW_LINE_SEPARATOR);
                }
            }

            //Salvar detalhes de nós de fronteira
            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("Nodes nas");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Bordas");

            fileWriter.append(NEW_LINE_SEPARATOR);
            fileWriter.append("Cluster");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Node");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lat");
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append("Lon");
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (int a = 0; a < (clusters.size()); a++) {

                for (int n = 0; n < clusters.get(a).getNosBordaFrom().size(); n++) {

                    fileWriter.append("c " + a);
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(clusters.get(a).getNosBordaFrom().get(n) + " to " + clusters.get(a).getNosBordaTo().get(n));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(clusters.get(a).getNosBordaLat().get(n) + "");
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(clusters.get(a).getNosBordaLon().get(n) + "");

                    fileWriter.append(NEW_LINE_SEPARATOR);
                }

            }

            System.out.println("OK: CSV O-DMatrixClusters " + name + " salvo!");

        } catch (IOException e) {
            System.out.println("ERROR: CsvFileWriter de " + name);
            //e.printStackTrace();
            return false;
        } finally {

            try {
                fileWriter.flush();
                fileWriter.close();
                // return true;
            } catch (IOException e) {
                System.out.println("ERROR: While flushing/closing fileWriter de " + name);
                // e.printStackTrace();
                return false;
            }

        }
        return true;
    }

    public String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));

    }

    public void encontrarNosBordaClusters(Mapping mapa) {

        DescriptiveStatistics stats = new DescriptiveStatistics();

        //de cluster em cluster
        for (int c = 0; c < clusters.size(); c++) {
            //clusters.get(c).encontrarNosBorda(BordaM, mapa);  @deprecated
            stats.addValue(clusters.get(c).encontrarNosBordaREDUX(mapa));
        }

        System.out.println("OK: Encontro nós em bordas de clusters! " + horaAtual() + "\n");
        System.out.println("STATS: Borda de cluster (Nodes). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

    }

    public void gerarScriptRgoogleMaps(VirtualSensors vt, Mapping map, double perctBorda) {

        /*geraRScript ger = new geraRScript();

        ger.setNomeArquivo("Nos borda de cluster.r");

        ger.criarArquivoRggmapFromClusters(clusters);

        System.out.println("PROC: Reduzindo número de nós borda dos clusters... ");

        int cont = 0;
        int contTotal = 0;

        for (int c = 0; c < clusters.size(); c++) {
            cont = cont + clusters.get(c).limitarNumArestasDeSaida(vt, perctBorda);
            contTotal += clusters.get(c).getNumeroNosBorda();
        }

        System.out.println("OK: Reduziu " + cont + "/(" + (contTotal + cont) + ") de nós borda dos clusters! " + horaAtual());

        ger.setNomeArquivo("Nos borda de cluster REDUX.r");

        ger.criarArquivoRggmapFromClusters(clusters); 
        wont work
        */

    }

    public void resetContagensBlocos(int bat, int numClust) { //para após setar clusters dos blocos, iniciar novas contagens de ODMatrix (com batches)

        for (int b = 0; b < blocos.size(); b++) {
            blocos.get(b).resetContagensBloco(discretTemporal, bat);
        }

        quadsLatPart = new ArrayList<>();
        quadsLonPart = new ArrayList<>();
        quadsLatCheg = new ArrayList<>();
        quadsLonCheg = new ArrayList<>();
        batchDaViagem = new ArrayList<>();

        ODmatrixClusters = new double[numClust][numClust][discretTemporal];
        ODmatrixClustersBatch = new double[numClust][numClust][discretTemporal][batchSize];
        for (int te = 0; te < discretTemporal; te++) {
            for (int a = 0; a < numClust; a++) {
                for (int b = 0; b < numClust; b++) {
                    ODmatrixClusters[a][b][te] = 0;
                    for (int bat2 = 0; bat2 < batchSize; bat2++) {
                        ODmatrixClustersBatch[a][b][te][bat2] = 0;
                    }
                }
            }
        }

        System.out.println("OK: Retirou dados de contagem dos blocos!");
    }

    public void loadDataset5RedefinirClusters(String locate) {

        System.out.println("PROC: Lendo dataset5 para importar clusterização... " + horaAtual());

        Scanner scanner;
        String[] r;
        String aux;

        ArrayList<Double> lat = new ArrayList<>();
        ArrayList<Double> lon = new ArrayList<>();
        ArrayList<Boolean> orig = new ArrayList<>();
        ArrayList<Integer> cluster = new ArrayList<>();

        int cont = 0;
        try {                                    //ler arquivo de definição de clusters
            scanner = new Scanner(new File(locate));
            scanner.useDelimiter("\n");
            while (scanner.hasNext()) {
                aux = scanner.next();
                if (cont > 8) {
                    r = aux.split(",");
                    lat.add(Double.valueOf(r[1]));
                    lon.add(Double.valueOf(r[2]));
                    orig.add(r[3].replace(" ", "").equals("orig"));
                    // if(cluster.indexOf(Integer.valueOf(r[4].substring(7)))==-1){
                    cluster.add(Integer.valueOf(r[4].substring(7).replace(" ", ""))); //cluster
                    //     System.out.println(cluster.get(cluster.size()-1));
                    // }
                }
                cont++;
            }

        } catch (FileNotFoundException ex) {
           // Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.criarBlocos();

        int quantClusters = cluster.get(0);  //encontra cluster de maior indice
        for (int l = 0; l < lat.size(); l++) {
            if (cluster.get(l) > quantClusters) {
                quantClusters = cluster.get(l); //encontra cluster de maior indice

            }
        }
        quantClusters++;

        numeroClusters = (short) quantClusters;
        System.out.println("INFO: " + numeroClusters + " clusters recuperados do arquivo do weka");
        int registrosPerd = 0;
        boolean regPerd = false;

        for (int l = 0; l < lat.size(); l++) //de registro em registro
        {
            regPerd = true;
            for (int b = 0; b < blocos.size(); b++) //de bloco em bloco
            {
                if (blocos.get(b).pontoPertenceAoBloco(lat.get(l), lon.get(l))) { //se ponto pertence ao bloco

                    blocos.get(b).acumularEscolhaCluster(quantClusters, cluster.get(l)); //acumula escolha de cluster
                    if (orig.get(b)) {
                        blocos.get(b).origensContMais(0, -1);   //acrescentando contagem de origem
                    } else {
                        blocos.get(b).destinosContMais(0, -1);  //acrescentando contagem de Destino
                    }
                    b = blocos.size(); //passa para próximo registro
                    regPerd = false;
                }
            }
            if (regPerd) {
                registrosPerd++;
            }
        }

        if (registrosPerd > 0) {
            System.out.println("ALERT: " + registrosPerd + "/" + lat.size() + " (" + ((100.0 * registrosPerd) / lat.size() * 1.0) + "%) registros do arquivo do weka não encontraram bloco correspondente!");
        }

        int blocPerd = 0;
        int s;
        for (int b = 0; b < blocos.size(); b++) {
            s = blocos.get(b).definirClusterPredominante();
            if (s == -1) {
                blocPerd++;
            }
        }

        System.out.println("ALERT: " + blocPerd + "/" + blocos.size() + " (" + 100.0 * blocPerd / (blocos.size() * 1.0) + "%) blocos sem cluster predominante!");

        blocosBackup = blocos;

        for (int b = 0; b < blocos.size(); b++) {
            for (int t = 0; t < discretTemporal; t++) {
                contagemDoBlocoOrig[blocos.get(b).getIndexLat()][blocos.get(b).getIndexLon()][t] += blocos.get(b).getOrigensCont(t);
                contagemDoBlocoDest[blocos.get(b).getIndexLat()][blocos.get(b).getIndexLon()][t] += blocos.get(b).getDestinosCont(t);
            }
        }

        descobrirBordas();
        cortarBordas();

        clusters = new ArrayList<>();
        for (int a = 0; a < numeroClusters; a++) {
            clusters.add(new cluster());                       //criando clusters novos
        }

        blocosBackup = new ArrayList<>();
        for (int b = 0; b < blocos.size(); b++) {
            //  if (blocos.get(b).getClusterPref() == -1) {
            blocosBackup.add(blocos.get(b));
            //  }
        }

        int blocosFora = 0;
        int cpref = -1;
        int indLat = -1;
        int indLon = -1;
        for (int b = 0; b < blocos.size(); b++) {           //blocos com cluster são excluidos. Ficam os que sobraram.
            cpref = -1;
            if (blocos.get(b).getClusterPref() != -1) {
                cpref = blocos.get(b).getClusterPref();
                clusters.get(cpref).addBloco(blocos.get(b));
                indLat = blocos.get(b).getIndexLat();
                indLon = blocos.get(b).getIndexLon();
                blocos.remove(b);
                if (b > 0) {
                    b--;
                }
            } else {
                indLat = blocos.get(b).getIndexLat();
                indLon = blocos.get(b).getIndexLon();
                blocosFora++;
            }

            // clusterDoBloco[blocos.get(b).getIndexLat()][blocos.get(b).getIndexLon()] = blocos.get(b).getClusterPref();
            clusterDoBloco[indLat][indLon] = (byte) cpref;
            //blocos.remove(r);
        }

        if (blocosFora != blocosBackup.size()) {
            System.out.println("END: Redefiniu clusters de acordo com registro do weka! (" + blocosFora + "/" + blocosBackup.size() + " blocos fora) " + horaAtual());
        } else {
            System.out.println("\nERROR: Clusters de acordo com registro do weka FALHOU! Todos os blocos ficaram fora!\n ");
        }

        //blocos = blocosBackup;
        //  kmeans(); //para adicionar blocos que sobraram
        System.out.println("END2: Após adequação com kmeans, " + blocos.size() + "/" + blocosFora + " blocos fora");

        ArrayList<Double> lats = new ArrayList<>();
        ArrayList<Double> lons = new ArrayList<>();
        ArrayList<String> name = new ArrayList<>();

        for (int c = 0; c < clusters.size(); c++) {
            System.out.println(clusters.get(c).getStrCentroide());
            lats.add(clusters.get(c).getCentroLat());
            lons.add(clusters.get(c).getCentroLon());
            name.add("c" + c);
        }

        while (lons.size() > 0) {

            int menor = 0;
            for (int x = 1; x < lats.size(); x++) {
                if (lons.get(x) < lons.get(menor)) {
                    menor = x;

                }
            }

            System.out.println(lats.get(menor) + "," + lons.get(menor) + ": " + name.get(menor));

            lats.remove(menor);
            lons.remove(menor);
            name.remove(menor);
        }

        blocos = blocosBackup;

        // calcMatrixClust(); Verdadeira matrix de clusters deve ser calculada com os dados totais (cria batches, encontra variâncias)
    }

    /**
     * @return the numeroClusters
     */
    public int getNumeroClusters() {
        return numeroClusters;
    }

    public double variancia(double[] x) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int z = 0; z < x.length; z++) {
            stats.addValue(x[z]);
        }
        return stats.getVariance();
    }

    public void zerarMatrizPathLink(int discretTemp) {

        for (int t = 0; t < discretTemp; t++) {
            for (int a = 0; a < numeroClusters; a++) {
                for (int b = 0; b < numeroClusters; b++) {

                    ODparFromNode[a][b][t] = new ArrayList<>();
                    ODparToNode[a][b][t] = new ArrayList<>();
                    ODparIndArestaGeral[a][b][t] = new ArrayList<>();
                    ODparArestaCont[a][b][t] = new ArrayList<>();
                    ODparPassagens[a][b][t] = 0;
                }
            }
        }
    }

    public int acrescentarMatrizPathLink(Mapping map, int qPLat, int qPLon, int qPLatD, int qPLonD, ArrayList<Integer> posNodes, int t) {

        int o = pertenceAoCluster(qPLat, qPLon);
        int d = pertenceAoCluster(qPLatD, qPLonD);
        int c = 0;

        for (int x = 0; x < posNodes.size() - 1; x++) {
            c += addLinkToODPar(map, posNodes.get(x), posNodes.get(x + 1), o, d, t);
        }

        if (c > 0) { //acrescentou em alguma aresta
            ODparPassagens[o][d][t]++; //mais uma contagem desse par OD
            return c / c;
        } else {
            return 0;
        }
    }

    public int addLinkToODPar(Mapping map, int indFrom, int indTo, int a, int b, int t) {

        if (a == -1 || b == -1 || t > discretTemporal) {
            return 0;
        }

        if (t == 16 || t == 8) //ATENÇÃO! GAMBIARRA PARA GERAR FUSÃO DE t16 e t8 em casa extra dos vetores. Modificar t para gerar outras fusões.
        {
            addLinkToODPar(map, indFrom, indTo, a, b, discretTemporal);
        }

        int indiceArestaG = map.getArestaIndex(indFrom, indTo);

        if (indiceArestaG == -1) {
            return 0;
        }

        //  System.out.println("a="+a+" b="+b);
        for (int x = 0; x < ODparFromNode[a][b][t].size(); x++) {                    //passando pelas arestas já conhecidas
            if (ODparFromNode[a][b][t].get(x) == indFrom && ODparToNode[a][b][t].get(x) == indTo) {  //se estiver tentando inserir em aresta já conhecida

                //ODparPassagens[a][b]++; //mais uma contagem desse par OD
                ODparArestaCont[a][b][t].set(x, ODparArestaCont[a][b][t].get(x) + 1.0); //mais uma contagem para a aresta

                //ODparIndArestaGeral[a][b].set(x, SAME);
                return 1;
            }
        }

        //não encontrou nas arestas cadastradas. Novo registro
        ODparFromNode[a][b][t].add(indFrom);
        ODparToNode[a][b][t].add(indTo);
        ODparIndArestaGeral[a][b][t].add(indiceArestaG);
        ODparArestaCont[a][b][t].add(1.0);
        //ODparPassagens[a][b]=1;

        return 1;
    }

    public void normalizarPathLinkMatrix(int discretTemp) {

        if (this.ODparPassagensBACKUP == null) {
            ODparPassagensBACKUP = new int[numeroClusters][numeroClusters][discretTemp];
            for (int c1 = 0; c1 < numeroClusters; c1++) {
                System.arraycopy(ODparPassagens[c1], 0, ODparPassagensBACKUP[c1], 0, numeroClusters);
            }
        }

        for (int t = 0; t < discretTemp; t++) {
            for (int c1 = 0; c1 < numeroClusters; c1++) {
                for (int c2 = 0; c2 < numeroClusters; c2++) {
                    for (int x = 0; x < ODparFromNode[c1][c2][t].size(); x++) {

                        if ((ODparArestaCont[c1][c2][t].get(x) / ((double) ODparPassagens[c1][c2][t])) > 1 || (ODparArestaCont[c1][c2][t].get(x) / ((double) ODparPassagens[c1][c2][t])) < 0) {
                            System.out.println("ERROR: " + ODparArestaCont[c1][c2][t].get(x) + "(ODparArestaCont[" + c1 + "][" + c2 + "].get(" + x + "))/" + ODparPassagens[c1][c2][t] + "(ODparPassagens[" + c1 + "][" + c2 + "]) > 1.0 (normalizarPathLinkMatrix)");
                        }
                        //ODparPassagens[c1][c2] = 1;

                        ODparArestaCont[c1][c2][t].set(x, ODparArestaCont[c1][c2][t].get(x) / ((double) ODparPassagens[c1][c2][t]));

                    }
                }
            }
        }

        for (int t = 0; t < discretTemp; t++) {
            for (int c1 = 0; c1 < numeroClusters; c1++) {
                for (int c2 = 0; c2 < numeroClusters; c2++) {
                    ODparPassagens[c1][c2][t] = 1;
                }
            }
        }

        System.out.println("OK: Normalizou matriz ODparArestaCont!");
    }

    public double getODParArestaCont(int o, int d, int t, int pos) {
        if (pos > ODparArestaCont[o][d][t].size()) {
            System.out.println("Size=" + ODparArestaCont[o][d][t].size() + ". Tentando get de " + pos + " o=" + o + "; d=" + d + "; t=" + t);
        }
        return ODparArestaCont[o][d][t].get(pos);
    }

    public double getProbUsoArestaPorParODeIndexAresta(int o, int d, int t, int indexAresta) {

        int ind = ODparIndArestaGeral[o][d][t].indexOf(indexAresta);
        if (ind == -1) {
            return 0.0;
        } else {
            return getODParArestaCont(o, d, t, ind);
        }

    }

    public int getODparArestaIndexGeralAresta(int o, int d, int t, int pos) {
        return this.ODparIndArestaGeral[o][d][t].get(pos);
    }

    public ArrayList<Integer> encontrarIndicesArestasVetor(int o, int d, int t, int[] sensorAresta) {
        //ODparIndAresta(indice geral) -> (indice vetor sensorAresta)
        ArrayList<Integer> ind = new ArrayList<>();

        for (int a = 0; a < discretTemporal; a++) {
            int cont2 = 0;
            for (int c = 0; c < clusters.size(); c++) {
                for (int c2 = 0; c2 < clusters.size(); c2++) {
                    try {
                        cont2 += ODparIndArestaGeral[c][c2][a].size();
                    } catch (Exception e) {
                        System.out.println("ERROR: ODparIndArestaGeral[" + c + "][" + c2 + "][" + a + "]");
                    }
                }
            }
            // System.out.println("[t="+a+"] - contagem "+cont2);
        }
        int cont=0;
        for (int x = 0; x < ODparIndArestaGeral[o][d][t].size(); x++) {
            for (int sa = 0; sa < sensorAresta.length; sa++) {
                if (ODparIndArestaGeral[o][d][t].get(x) == sensorAresta[sa]) {
                    ind.add(sa); //indice no vetor sensorAresta
                    //ind.add(x);
                    sa = sensorAresta.length;
                }
            }

            if (ind.size() <= x) //x=0 e ind.size=0: não encontrou, então add -1 para manter ordem
            {
                cont++;
                ind.add(-1);
            }
        }

       // if(cont>0)  System.out.println("ALERT: Par OD "+o+","+d+". De "+ODparIndArestaGeral[o][d][t].size()+", "+cont+" arestas não foram localizadas para "+sensorAresta.length+" sensores!");        
        return ind;
    }

    public void printStatsVariancia(int tempo) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int c = 0; c < numeroClusters; c++) {
            for (int c2 = 0; c2 < numeroClusters; c2++) {
                stats.addValue(ODmatrixClustersVARIANCE[c][c2][tempo]);
            }
        }

        System.out.println("STATS: ODmatrixClustersVariance (t" + tempo + "). "
                + "Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; "
                + "mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; "
                + "perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

    }

    public static double[][] inverterMatriz(double a[][]) {

        int n = a.length;
        double x[][] = new double[n][n];
        double b[][] = new double[n][n];
        int index[] = new int[n];
        for (int i = 0; i < n; ++i) {
            b[i][i] = 1;
        }

        // Transform the matrix into an upper triangle
        gaussian(a, index);
        // Update the matrix b[i][j] with the ratios stored
        for (int i = 0; i < n - 1; ++i) {
            for (int j = i + 1; j < n; ++j) {
                for (int k = 0; k < n; ++k) {
                    b[index[j]][k]
                            -= a[index[j]][i] * b[index[i]][k];
                }
            }
        }

        // Perform backward substitutions
        for (int i = 0; i < n; ++i) {
            x[n - 1][i] = b[index[n - 1]][i] / a[index[n - 1]][n - 1];
            for (int j = n - 2; j >= 0; --j) {
                x[j][i] = b[index[j]][i];
                for (int k = j + 1; k < n; ++k) {
                    x[j][i] -= a[index[j]][k] * x[k][i];
                }
                x[j][i] /= a[index[j]][j];
            }
        }
        return x;
    }

    // Method to carry out the partial-pivoting Gaussian elimination.  Here index[] stores pivoting order.
    public static void gaussian(double a[][], int index[]) {
        int n = index.length;
        double c[] = new double[n];

        // Initialize the index
        for (int i = 0; i < n; ++i) {
            index[i] = i;
        }

        // Find the rescaling factors, one from each row
        for (int i = 0; i < n; ++i) {
            double c1 = 0;
            for (int j = 0; j < n; ++j) {
                double c0 = Math.abs(a[i][j]);
                if (c0 > c1) {
                    c1 = c0;
                }
            }
            c[i] = c1;
        }

        // Search the pivoting element from each column
        int k = 0;
        for (int j = 0; j < n - 1; ++j) {
            double pi1 = 0;
            for (int i = j; i < n; ++i) {
                double pi0 = Math.abs(a[index[i]][j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) {
                    pi1 = pi0;
                    k = i;
                }
            }

            // Interchange rows according to the pivoting order
            int itmp = index[j];
            index[j] = index[k];
            index[k] = itmp;
            for (int i = j + 1; i < n; ++i) {
                double pj = a[index[i]][j] / a[index[j]][j];

                // Record pivoting ratios below the diagonal
                a[index[i]][j] = pj;

                // Modify other elements accordingly
                for (int l = j + 1; l < n; ++l) {
                    a[index[i]][l] -= pj * a[index[j]][l];
                }
            }
        }
    }

}
