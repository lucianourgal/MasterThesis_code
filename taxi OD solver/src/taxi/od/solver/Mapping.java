/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package taxi.od.solver;

import auxs.GeraGraficos;
import auxs.geradordeRede2;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Luciano
 */
public class Mapping implements Serializable {

    /**
     * @param ROIsizeM the ROIsizeM to set
     */
    public void setROIsizeM(int ROIsizeM) {
        this.ROIsizeM = ROIsizeM;
    }

    private String[] node_id;
    private Double[] node_lat;
    private Double[] node_lon;
    private int contNodes = 0;

    private int ROIsizeM;
    private int ROInumberLat;
    private int ROInumberLon;

    private String[] way_id;
    private String[] way_name;
    private ArrayList<String>[] way_nodes;
    private String[] way_kind;
    private boolean[] oneWay;

    private int indexNodes;
    private int indexWays;
    private int indexEnd;
    private final int wayCount;
    private double fatorRedux = 0.8;

    private boolean calcWaysPorNode; //carga grande processamento
    private int[] waysPorNode;
    private Double menorLat;
    private Double maiorLat;
    private Double menorLon;
    private Double maiorLon;

    private boolean vizinhosCadastrados = false;
    //private int[][] vizinhosIndex;
    //private String[][] vizinhosCod;
    //private int[] contVizinhos;
    private ArrayList<Integer>[] vizinhosIndex;
    private ArrayList<String>[] vizinhosCod;
    private ArrayList<Integer>[] vizinhosNodeAresta;

    private int[][] gemeosIndex;
    private int[] contGemeos;

    private ArrayList<String> fromNod;
    private ArrayList<String> toNod;
    boolean temArestas = false;

    private ArrayList<Integer> fromNodI;
    private ArrayList<Integer> toNodI;
    private ArrayList<Double> arestaExtensaoMETROS;
    private ArrayList<Double> arestaExtensaoGRAUS;
    private ArrayList<Double> arestaCoefAngular;
    private ArrayList<Double> arestaCoefAngularInv;
    private ArrayList<Double> arestaCoefLinear;
    private ArrayList<String> arestaKind;
    private ArrayList<String> arestaCodSUMO;
    
    
    
    public int[] calcNSLPcorrelacao(int qtdeFinal, int [] arestasOpcao, ODMatrix odm){
    if(fluxos==null)
        criarCorrelacaoArestas(odm);
        
        System.out.println("PROC: Fazendo NSPL por correlação... "+horaAtual());
    boolean [] disponivel = new boolean[arestasOpcao.length];
    PearsonsCorrelation p = new PearsonsCorrelation();
    for(int x=0;x<arestasOpcao.length;x++)
        disponivel[x] = false;
    
    int [] finalx = new int[qtdeFinal];
    finalx[0] = arestasOpcao[0];
    disponivel[0] = false;
    int esc = -1;
    
    for(int x=1;x<qtdeFinal;x++){
    double menosRelac = 9999999.4;
    double soma = 0.0;
    //entre arestas disponiveis
    for(int x2=1;x2<arestasOpcao.length;x2++){
        if(disponivel[x2])
    //encontra a menos relacionada com o conjunto
            for(int y=0;y<x;y++){
                soma += p.correlation(fluxos[x2], fluxos[finalx[x]]);//correlacaoArestas[x2][finalx[x]];
            }
    
        if(soma<menosRelac){
            menosRelac = soma;
            esc = x2;
        }
        
    }
    
    
    //adiciona nova aresta
    disponivel[esc] = false;
    finalx[x] = arestasOpcao[esc];
    }
    
    
    return finalx;
    }
    
    double [][] fluxos;
    
    public void criarCorrelacaoArestas(ODMatrix odm){
    System.out.println("PROC: Calculando correlacao entre "+fromNodI.size()+" arestas... "+horaAtual());
 //correlacaoArestas = new float[fromNodI.size()][fromNodI.size()];
            
    //criar várias ODM
    double [][][] odms = new double[odm.getNumeroClusters()][odm.getNumeroClusters()][7];
    for(int o=0;o<odm.getNumeroClusters();o++)
        for(int d=0;d<odm.getNumeroClusters();d++)
            for(int c=0;c<7;c++)
                odms[o][d][c] = Math.random()*30;
    
    //distribuir pelos fluxos
    fluxos = new double[fromNodI.size()][7];
    for(int o=0;o<odm.getNumeroClusters();o++)
        for(int d=0;d<odm.getNumeroClusters();d++)
            for(int a=0;a<fromNodI.size();a++)
                for(int c=0;c<7;c++)
                    fluxos[a][c] += odm.getProbUsoArestaPorParODeIndexAresta(o, d, 8, a)*odms[o][d][c];
    
    //calcular correlação entre pares
    /*PearsonsCorrelation p = new PearsonsCorrelation();
    for(int a1=0;a1<fromNodI.size();a1++)
        for(int a2=0;a2<fromNodI.size();a2++)
            correlacaoArestas[a1][a2] = (float)p.correlation(fluxos[a1], fluxos[a2]);*/
    
    this.salvarDat(OSMFileLocation);
    
    }
    
    
    
    
    public double getArestaExtensaoMETROS(int aresta) {
        return arestaExtensaoMETROS.get(aresta);
    }

    public double getArestaExtensaoMETROS(int node1, int node2) {
        int i = this.getArestaIndex(node1, node2);
        if (i != -1) {
            return arestaExtensaoMETROS.get(i);
        }
        return i;
    }
    
    public double getArestaExtensaoGRAUS(int aresta) {
        return arestaExtensaoGRAUS.get(aresta);
    }

    public double getArestaExtensaoGRAUS(int node1, int node2) {
        int i = this.getArestaIndex(node1, node2);
        if (i != -1) {
            return arestaExtensaoGRAUS.get(i);
        }
        return i;
    }

    public void showStatsTamanhoArestas(){
    System.out.println("PROC: Calculando Stats tamanho arestas...");
    ArrayList<Double> ar = new ArrayList<>();
    double [] n = new double[fromNodI.size()];
    
    for(int x=0;x<fromNodI.size();x++){
        ar.add(this.distanciaMNodes(fromNodI.get(x), toNodI.get(x)));
        n[x] = ar.get(x);
    }
    
    printStats(ar,"tamanhoArestas");
    
    GeraGraficos g = new GeraGraficos();
    g.criarHistograma("Graficos", "tamanho das arestas", n, 20); //pasta, nome, valores, classes
    
    }
    
    
    
     public static void printStats(ArrayList<Double> d, String name) {

        DescriptiveStatistics ds = new DescriptiveStatistics();
        for (int x = 0; x < d.size(); x++) {
            ds.addValue(d.get(x));
        }
        printStats(ds, name);
    }

    public static void printStats(DescriptiveStatistics d, String name) {
        DecimalFormat df2 = new DecimalFormat(".##");
        String t = "STATS: " + name + ": Min. " + df2.format(d.getMin()) + "; Mean. " + df2.format(d.getMean())+"; ";

        for (int p = 5; p < 100; p = p + 15) {
            t = t + "p" + p + " " + df2.format(d.getPercentile(p)) + "; ";
        }

        System.out.println(t + "; Max. " + df2.format(d.getMax()) + "; ");
    }
    
    
    public ArrayList<Integer> encontrarIndexWaysPorNomeDeRua(ArrayList<String> ruas) {
        ArrayList<Integer> index = new ArrayList<>();
        ArrayList<String> norm = new ArrayList<>();

        int ok = 0;

        //normaliza ruas
        for (int r = 0; r < ruas.size(); r++) {
            norm.add(padronizarStringRua(ruas.get(r)));
        }
        ruas = new ArrayList<>();
        for (int r = 0; r < norm.size(); r++) {
            ruas.add(norm.get(r));
        }
        //normaliza nomes de ruas
        norm = new ArrayList<>();
        for (int w = 0; w < this.getWayCount(); w++) {
            norm.add(padronizarStringRua(way_name[w]));
        }

        //way_id
        boolean find;
        for (int r = 0; r < ruas.size(); r++) {
            find = false;

            for (int w = 0; w < this.getWayCount(); w++) {
                if (compNomeRua(norm.get(w), ruas.get(r))) {//if (norm.get(w).equals(ruas.get(r))) {
                    index.add(w);
                    w = this.getWayCount();
                    find = true;
                    ok++;
                }
            }//fim de loop por ways
            if (!find) {
                index.add(-1);
                // System.out.println(ruas.get(r));
            }
        }
        System.out.println("OK: De " + ruas.size() + " ruas, encontrou " + ok + " ways! Existem " + this.getWayCount() + " ways no objeto Mapa. (Mapping.encontrarIndexWaysPorNomeDeRua) " + horaAtual());
        return index;
    }

    private boolean compNomeRua(String s1, String s2) {
        String[] s1T = new String[1];
        String[] s2T = new String[1];
        if (s1.contains("-")) {
            s1T = s1.split("-");
            for (int a = 0; a < s1T.length; a++) {
                s1T[a] = padronizarStringRua(s1T[a]);
            }

        } else if (s1.contains("/")) {
            s1T = s1.split("/");
            for (int a = 0; a < s1T.length; a++) {
                s1T[a] = padronizarStringRua(s1T[a]);
            }

        } else {
            s1T = new String[1];
            s1T[0] = s1;
        }

        if (s2.contains("-")) {
            s2T = s2.split("-");
            for (int a = 0; a < s2T.length; a++) {
                s2T[a] = padronizarStringRua(s2T[a]);
            }

        } else if (s2.contains("/")) {
            s2T = s2.split("/");
            for (int a = 0; a < s2T.length; a++) {
                s2T[a] = padronizarStringRua(s2T[a]);
            }
        } else {
            s2T = new String[1];
            s2T[0] = s2;
        }

        for (String s1T1 : s1T) {
            for (String s2T1 : s2T) {
                if (s1T1.equals(s2T1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String padronizarStringRua(String s) {
        s = s.toUpperCase();
        s = s.replace("  ", " ");
        s = s.replace("  ", " ");
        s = s.replace(".", "");
        s = s.replace(",", "");
        s = s.replace("“", "");
        s = s.replace("”", "");
        s = s.replace("'", "");
        s = s.replace("(", "##");
        s = s.split("##")[0];
        if (s.substring(0, 1).equals(" ")) {
            s = s.substring(1);
        }

        if (s.substring(s.length() - 1, s.length()).equals(" ")) {
            s = s.substring(0, s.length() - 1);
        }

        s = s.replace("FIRST ", "1 ");
        s = s.replace("FST ", "1 ");
        s = s.replace("SECOND ", "2 ");
        s = s.replace("SND ", "2 ");
        s = s.replace("THIRD ", "3 ");
        s = s.replace("TRD ", "3 ");
        s = s.replace("FOURTH ", "3 ");
        s = s.replace("FTH ", "3 ");

        s = s.replace("AVENUE", "AVE");
        s = s.replace("STREET", "ST");

        for (int a = 0; a < 10; a++) {
            s = s.replace(a + "ND ", a + " ");
            s = s.replace(a + "TH ", a + " ");
            s = s.replace(a + "RD ", a + " ");
            s = s.replace(a + "ST ", a + " ");
        }
        // System.out.println(s);
        return s;

    }

    public int calcIndexArestaDatasetNYC(int wayRua, int wayFrom, int wayTo, String direction) {

        if (wayRua < 0 || wayFrom < 0 || wayTo < 0) {
            return -1;
        }

        int from = -1, to = -1;
        direction = direction.replace(" ", "");
        direction = direction.replace("-SER", "");
        direction = direction.replace("-MAIN", "");

        for (int w = 0; w < this.way_nodes[wayRua].size(); w++) {

            for (int w2 = 0; w2 < this.way_nodes[wayFrom].size(); w2++) {
                if (way_nodes[wayRua].get(w).equals(way_nodes[wayFrom].get(w2))) {
                    from = this.getNodeIndex(way_nodes[wayRua].get(w));
                    w2 = way_nodes[wayFrom].size();
                }
            }
            for (int w2 = 0; w2 < this.way_nodes[wayTo].size(); w2++) {
                if (way_nodes[wayRua].get(w).equals(way_nodes[wayTo].get(w2))) {
                    to = this.getNodeIndex(way_nodes[wayRua].get(w));
                    w2 = way_nodes[wayTo].size();
                }
            }

        }

        switch (direction) {
            case "NB":
                //indo para norte
                if (node_lat[from] < node_lat[to]) {
                    return this.getArestaIndex(from, to);
                } else {
                    return this.getArestaIndex(to, from);
                }
            case "SB":
                //indo para sul
                if (node_lat[from] > node_lat[to]) {
                    return this.getArestaIndex(from, to);
                } else {
                    return this.getArestaIndex(to, from);
                }
            case "WB":
                //indo para WEST/Oeste
                if (node_lon[from] > node_lon[to]) {
                    return this.getArestaIndex(from, to);
                } else {
                    return this.getArestaIndex(to, from);
                }
            case "EB":
                //indo para EAST/Leste
                if (node_lon[from] < node_lon[to]) {
                    return this.getArestaIndex(from, to);
                } else {
                    return this.getArestaIndex(to, from);
                }
            default:
                return -3;
        }

    }

    public ArrayList<Integer> calcIndexNOSQuePassaPelasArestas(ArrayList<Double> lats, ArrayList<Double> lons, double metrosR,
            boolean validacao, double distanciaValidacao, boolean ahead, int profundidadeBusca) {

        int trueConnections = 0;
        ArrayList<Double> la = new ArrayList<>();
        ArrayList<Double> lo = new ArrayList<>();
        int aux;
        //retirar pontos muito próximos de nós, para evitar confusao
        if(validacao){
        for (int x = 0; x < lats.size(); x++) {
            aux = this.identificarNodeMaisProximoIndex(lats.get(x), lons.get(x), 2.5, 1.0);
            if (aux == -1) {
                la.add(lats.get(x));
                lo.add(lons.get(x));
            }
        }
        lats = la;
        lons = lo;
        }

        ArrayList<Integer> arestas = calcIndexArestasQuePassa(lats, lons, metrosR, ahead, profundidadeBusca); //aqui a mágica acontece. Encontra arestas
        //System.out.println(arestas.size() +"arestas");
        ArrayList<Integer> nos = new ArrayList<>();

        for (int a = 0; a < arestas.size(); a++) {

            if (nos.isEmpty()) { 
                nos.add(fromNodI.get(arestas.get(a)));  //primeiro nó. OK
            } else if (!Objects.equals(fromNodI.get(arestas.get(a)), nos.get(nos.size() - 1))) { //se novo nó é diferente do anterior

                if (!Objects.equals(fromNodI.get(arestas.get(a)), nos.get(nos.size() - 2))) //se for diferente  de dois anteriores (nesse caso, está repetindo aresta)
                {
                    nos.add(fromNodI.get(arestas.get(a)));
                }
            } else {
                trueConnections++; //se for igual, é porque pegou duas arestas em sequência (e não precisa acrescentar outro nó a sequência)
            }
            //segundo nó 
            if (nos.size() < 2) { //primeira aresta sendo adicionada
                nos.add(toNodI.get(arestas.get(a)));
                trueConnections++;
            } else if (!Objects.equals(toNodI.get(arestas.get(a)), nos.get(nos.size() - 1))) { //se não está repetindo aresta
                nos.add(toNodI.get(arestas.get(a)));
                trueConnections++;
            }

        }//fim laço de aresta em aresta  

        //fazer validação de nós
        if (validacao) {
            int com = nos.size();
            ArrayList<Integer> novos = new ArrayList<>();
            for (int a = 0; a < nos.size(); a++) {
                for (int i = 0; i < lats.size(); i++) {
                    if (distanciaPontosMETROS(lats.get(i), lons.get(i), node_lat[nos.get(a)], node_lon[nos.get(a)]) < distanciaValidacao) {
                        i = lats.size();
                        novos.add(nos.get(a));
                    }
                }
            }
            nos = novos;
            // System.out.println("VALIDACAO: "+nos.size()+"/"+com+" nós mantidos após validacao! (Mapping.calcIndexNosQuePassaPelasArestas)");
        }

        /*System.out.println("OK: A partir de "+lats.size()+" pontos WGS84, "+trueConnections+"/"+(nos.size()-1)+" "
            + "("+(trueConnections*100)/(nos.size()-1)+"%) conexões de arestas ok! (Mapping.calcIndexNOSquePassa)"); */
        return nos;
    }

    
    public ArrayList<Integer> calcIndexNOSQuePassaPelasNosProximosDasArestas(ArrayList<Double> lats, ArrayList<Double> lons, double metrosR, double raioParaRandom) {

        ArrayList<Integer> nosInd = new ArrayList<>();
        int i;

        for (int a = 0; a < lats.size(); a++) {

            //i = identificarNodeMaisProximoIndex(lats.get(a), lons.get(a), metrosR, raioParaRandom);
            i = this.identificarArestaMaisProximoIndex(lats.get(a), lons.get(a), metrosR);

            if (i != -1){
            if(distanciaPontosMETROS(node_lat[fromNodI.get(i)], node_lon[fromNodI.get(i)], lats.get(a), lons.get(a)) 
                    <  distanciaPontosMETROS(node_lat[toNodI.get(i)], node_lon[toNodI.get(i)], lats.get(a), lons.get(a)))
                i = fromNodI.get(i);
            else
                i = toNodI.get(i);
            }
            
            if (i != -1) {
                if (nosInd.isEmpty()) {
                    nosInd.add(i);
                } else if (nosInd.get(nosInd.size() - 1) != i) { //adiciona se for diferente do anterior
                    nosInd.add(i);
                }
            }

        }//fim laço de nó em nó

        /*System.out.println("OK: A partir de "+lats.size()+" pontos WGS84, "+trueConnections+"/"+(nos.size()-1)+" "
            + "("+(trueConnections*100)/(nos.size()-1)+"%) conexões de arestas ok! (Mapping.calcIndexNOSquePassa)"); */
        return nosInd;
    }
    
    
    
    
    
    public ArrayList<Integer> calcIndexNOSQuePassaPelosNos(ArrayList<Double> lats, ArrayList<Double> lons, double metrosR, double raioParaRandom) {

        ArrayList<Integer> nosInd = new ArrayList<>();
        int i;

        for (int a = 0; a < lats.size(); a++) {

            i = identificarNodeMaisProximoIndex(lats.get(a), lons.get(a), metrosR, raioParaRandom);

            if (i != -1) {
                if (nosInd.isEmpty()) {
                    nosInd.add(i);
                } else if (nosInd.get(nosInd.size() - 1) != i) { //adiciona se for diferente do anterior
                    nosInd.add(i);
                }
            }

        }//fim laço de nó em nó

        /*System.out.println("OK: A partir de "+lats.size()+" pontos WGS84, "+trueConnections+"/"+(nos.size()-1)+" "
            + "("+(trueConnections*100)/(nos.size()-1)+"%) conexões de arestas ok! (Mapping.calcIndexNOSquePassa)"); */
        return nosInd;
    }

    public int getContArestas() {
        return fromNodI.size();
    }

    public String getSUMOEdgeCode(int nod1, int nod2) {
        //return getSUMOEdgeCode(node_id[nod1],node_id[nod2]);
        for (int a = 0; a < fromNodI.size(); a++) {
            if (fromNodI.get(a) == nod1) {
                if (toNodI.get(a) == nod2) {
                    return this.arestaCodSUMO.get(a);
                }
            }
        }

        return "FALHA";
    }

    public void printLocalizacoesArestas(String a1, String a2) {
        int ind1 = arestaCodSUMO.indexOf(a1);
        int ind2 = arestaCodSUMO.indexOf(a2);
        System.out.println(a1 + ": " + fromNodI.get(ind1) + "to " + toNodI.get(ind1) + "; " + a2 + ": " + fromNodI.get(ind2) + " to " + toNodI.get(ind2));
        printLocalizacoesAresta(ind1);
        printLocalizacoesAresta(ind2);
    }

    public void printLocalizacoesAresta(int index) {

        if (index == -1) {
            return;
        }

        System.out.println(node_lat[fromNodI.get(index)] + ", " + node_lon[fromNodI.get(index)]);
        System.out.println(node_lat[toNodI.get(index)] + ", " + node_lon[toNodI.get(index)]);
    }

    public String getSUMOEdgeCode(int codAresta) {
        //return getSUMOEdgeCode(node_id[nod1],node_id[nod2]);
        if (codAresta > -1 && codAresta < arestaCodSUMO.size()) {
            return this.arestaCodSUMO.get(codAresta);
        }

        return "FALHA";
    }

    public boolean existeAresta(int node_from, int node_to) {
        //return getSUMOEdgeCode(node_id[nod1],node_id[nod2]);
        for (int a = 0; a < fromNodI.size(); a++) {
            if (fromNodI.get(a) == node_from) {
                if (toNodI.get(a) == node_to) {
                    return true;
                }
            }
        }

        return false;
    }

     public int getArestaIndex(String node_from, String node_to){
     
     return getArestaIndex(getNodeIndex(node_from),getNodeIndex(node_to));
     
     }
    
    
    public int getArestaIndex(int node_from, int node_to) {
        //return getSUMOEdgeCode(node_id[nod1],node_id[nod2]);
        for (int a = 0; a < fromNodI.size(); a++) {
            if (fromNodI.get(a) == node_from) {
                if (toNodI.get(a) == node_to) {
                    return a;
                }
            }
        }
        
        if(fromNodI.size()<10)
            System.out.println("ERROR: fromNodI.size = "+fromNodI.size()+" (Mapping)");

        return -1;
    }

    public boolean existeVizinho(int node_from, int node_to, boolean usarGemeos) {
        //return getSUMOEdgeCode(node_id[nod1],node_id[nod2]);
        for (int a = 0; a < vizinhosIndex[node_from].size(); a++) {
            if (this.getVizinhosIndexDoNode(node_from, usarGemeos).get(a) == node_to) {
                return true;
            }
        }

        return false;
    }

    public boolean existeVizinhoPermitido(int node_from, int node_to, int aresta) {
        //return getSUMOEdgeCode(node_id[nod1],node_id[nod2]);
        if (aresta != -1 && vizinhosDaAresta.length > 0) {
            for (int a = 0; a < this.vizinhosDaAresta[aresta].size(); a++) {
                if (this.vizinhosDaAresta[aresta].get(a) == node_to) {
                    return true;
                }
            }
        } else {
            return existeVizinho(node_from, node_to, false);
        }

        return false;
    }

    public ArrayList<Integer> calcIndexArestasQuePassa(ArrayList<Double> lats, ArrayList<Double> lons, double metrosR, boolean ahead, int profBusca) {
        ArrayList<Integer> arestas = new ArrayList<>();
        int aux;
        double nextLat, nextLon;

        for (int a = 0; a < lats.size(); a++) {

            if (a < (lats.size() - 1)) {
                nextLat = lats.get(a + 1);
                nextLon = lons.get(a + 1);
            } else {
                nextLat = -1;
                nextLon = -1;
            }

            if (arestas.isEmpty()) {
                aux = getIndexArestaProxima(lats.get(a), lons.get(a), metrosR, -1, nextLat, nextLon, ahead,profBusca);
            } else {
                aux = getIndexArestaProxima(lats.get(a), lons.get(a), metrosR, arestas.get(arestas.size() - 1), nextLat, nextLon, ahead,profBusca);
            }

            if (aux != -1) {
                if (arestas.isEmpty()) //se é a primeira aresta
                {
                    arestas.add(aux);
                } else if (arestas.get(arestas.size() - 1) != aux) //se for diferente da aresta anterior
                {
                    arestas.add(aux);
                }
            }
        }
        return arestas;
    }

    public void lerEdgesXMLSumo(String arquivo) {

        int indexEdges = 0;
        int fimEdges = 100000;
        int tagId = 1, tagFrom = 3, tagTo = 5, tagKind = 9;

        ArrayList<String> forbiden = new ArrayList<>();
        forbiden.add("-161914512#2"); //161914497#2' and edge '48304404#1
        forbiden.add("377547848#0");
        forbiden.add("229972006#1"); //No connection between edge '161914496#0' and edge '229972006#1'.
        forbiden.add("-128988919#0");
        forbiden.add("128988919#0");
        forbiden.add("128988863#0");
        forbiden.add("-128988863#0");
        //regiao rota 649
        forbiden.add("-127264759#22");
        forbiden.add("127264766#8");
        forbiden.add("127264743#4");
        forbiden.add("127264759#22");
        forbiden.add("-127264766#8");
        forbiden.add("-127264743#4");
        forbiden.add("438795217#1");
        forbiden.add("-438795217#1");
        forbiden.add("-438795216");
        forbiden.add("438795216");
        forbiden.add("132418402#1");
        forbiden.add("-132418402#1");
        forbiden.add("-161237176#6");
        forbiden.add("161237176#6");
        forbiden.add("-128988811#0");
        forbiden.add("128988811#0");
        forbiden.add("-128988811#1");
        forbiden.add("128988811#1");
        forbiden.add("-128988881");
        forbiden.add("128988881");

        //forbiden.add("-475873376#1"); //perto do terminal. Nao conecta com canaleta, mesmo tendo connection
        forbiden.add("475873376#1"); //perto do terminal. Nao conecta com canaleta, mesmo tendo connection
        //forbiden.add("475873376#0"); //perto do terminal. Tambem problematica

        forbiden.add("-125207637#1"); //Rota 617. Passagem de canteiro

        //forbiden.add("-161917483"); //rua do terminal. Tem connection no xml, mas diz No connection between edge '48294478#1' and edge '-161917483'
        //<edge id="165612862#0" from="1771527905" to="1771527863" priority="4" type="highway.residential">
        //  0        1         2       3        4       5          6      7    8      9   
        System.out.println("PROC: Recuperando dados geográficos do xml SUMO... " + horaAtual());
        Scanner scanner;

        try {
            scanner = new Scanner(new File(arquivo));
            scanner.useDelimiter("\\Z");
            arquivo = scanner.next();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

        String[] linhas = arquivo.split("\n");
        String[] s;
        System.out.println("Total de " + linhas.length + " linhas no arquivo XML SUMO.");

        for (int x = 0; x < linhas.length; x++) {
            if (linhas[x].replace(" ", "").length() > 5) {
                // comp(linhas[x].substring(0, 4).replace(" ", ""),"<node".substring(0, 4));
                if (linhas[x].replace(" ", "").substring(0, 7).equals("<edgeid=\"".substring(0, 7))) {

                    if (linhas[x].contains("from=")) {
                        indexEdges = x;
                        x = linhas.length;

                    }
                }
            }
        }

        for (int x = indexEdges; x < linhas.length; x++) {
            if (linhas[x].length() > 3) {
                if (linhas[x].replace(" ", "").substring(0, 5).equals("<tlLogic".substring(0, 5))) {
                    fimEdges = x;
                    x = linhas.length;
                }
            }
        }

        ArrayList<String> edgeID = new ArrayList<>();
        ArrayList<String> edgeFrom = new ArrayList<>();
        ArrayList<String> edgeTo = new ArrayList<>();
        ArrayList<String> edgeKind = new ArrayList<>();

        for (int e = indexEdges; e < fimEdges; e++) {
            if (linhas[e].replace(" ", "").length() > 5) {
                if (linhas[e].replace(" ", "").substring(0, 7).equals("<edgeid=\"".substring(0, 7))) {

                    s = linhas[e].split("\"");
                    if (!(forbiden.contains(s[tagId]))) {
                        edgeID.add(s[tagId]);
                        edgeFrom.add(s[tagFrom]);
                        edgeTo.add(s[tagTo]);
                        edgeKind.add(s[tagKind].replace("highway.", ""));
                    }

                }
            }

        }

        fromNod = new ArrayList<>();
        toNod = new ArrayList<>();
        fromNodI = new ArrayList<>();
        toNodI = new ArrayList<>();
        arestaKind = new ArrayList<>();
        arestaCodSUMO = new ArrayList<>();
        arestaExtensaoMETROS = new ArrayList<>();
        arestaExtensaoGRAUS = new ArrayList<>();
        arestaCoefAngular = new ArrayList<>();
        arestaCoefAngularInv = new ArrayList<>();
        arestaCoefLinear = new ArrayList<>();
        int n1, n2;
        int naoEncontrados = 0;

        for (int e = 0; e < edgeID.size(); e++) {
            //System.out.println(hwaykinds.get((int)(Math.random()*hwaykinds.size()))+ " "+ edgeKind.get(e));
            if (!hwaykinds.contains(edgeKind.get(e))) {
                n1 = getNodeIndex(edgeFrom.get(e));
                n2 = getNodeIndex(edgeTo.get(e));

                if (n1 != -1 && n2 != -1) {
                    fromNod.add(edgeFrom.get(e));
                    toNod.add(edgeTo.get(e));
                    arestaKind.add(edgeKind.get(e));
                    fromNodI.add(n1);
                    toNodI.add(n2);
                    arestaCoefAngular.add(calcCoefAngularNodes(n1, n2));
                    arestaCoefLinear.add(calcCoefLinear(arestaCoefAngular.get(fromNodI.size()-1), n1));
                    arestaCoefAngularInv.add(1/arestaCoefAngular.get(arestaCoefAngular.size()-1));
                    arestaCodSUMO.add(edgeID.get(e));
                    arestaExtensaoMETROS.add(distanciaMNodes(n1, n2));
                    arestaExtensaoGRAUS.add(distanciaNodesGRAUS(n1, n2));
                } else {
                    naoEncontrados++;
                }
            }
        }

        System.out.println("OK! Leu total de " + edgeID.size() + " arestas de XML SUMO! Não encontrou " + naoEncontrados + " edges por nós não identificados!");

        //ler connections <connection from="397496776#2" to="164655539#3" fromLane="0" toLane="0" via=":4002895188_5_0" dir="l" state="m"/>
        connectionFrom = new ArrayList<>();
        connectionTo = new ArrayList<>();
        connectionFromIndex = new ArrayList<>();
        connectionToIndex = new ArrayList<>();

        for (int e = fimEdges; e < linhas.length; e++) {

            if (linhas[e].replace(" ", "").length() > 7) {
                if (linhas[e].replace(" ", "").substring(0, 7).equals("<connection=\"".substring(0, 7))) {
                    connectionFrom.add(linhas[e].split("\"")[1]);
                    connectionTo.add(linhas[e].split("\"")[3]);
                    connectionFromIndex.add(getArestaIndexComCodSUMO(connectionFrom.get(connectionFrom.size() - 1)));
                    connectionToIndex.add(getArestaIndexComCodSUMO(connectionTo.get(connectionTo.size() - 1)));
                }
            }

        }

        System.out.println("OK! Leu total de " + connectionFrom.size() + " conexões de arestas de XML SUMO! ");

    }

    public boolean existeConnectionEntreArestas(String ar1, String ar2) {

        for (int a = 0; a < connectionFrom.size(); a++) {
            if (connectionFrom.get(a).equals(ar1)) {
                if (connectionTo.get(a).equals(ar2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean existeConnectionEntreArestas(int ar1, int ar2) {

        for (int a = 0; a < connectionFrom.size(); a++) {
            if (connectionFromIndex.get(a) == ar1) {
                if (connectionToIndex.get(a) == ar2) {
                    return true;
                }
            }
        }
        return false;
    }

    ArrayList<String> connectionFrom = new ArrayList<>();
    ArrayList<String> connectionTo = new ArrayList<>();
    ArrayList<Integer> connectionFromIndex = new ArrayList<>();
    ArrayList<Integer> connectionToIndex = new ArrayList<>();

    public int getArestaIndexComCodSUMO(String codSUMO) {

        return this.arestaCodSUMO.indexOf(codSUMO);

    }

    public void relatArestaSUMO(String aresta) {
        String cods = "";

        for (int x = 0; x < this.arestaCodSUMO.size(); x++) {
            if (aresta.equals(arestaCodSUMO.get(x))) {
                //from="266971150" to="266971151"
                System.out.println("<edge id=\"" + aresta + "\" from=\"" + fromNod.get(x) + "\" to\"" + toNod.get(x) + "\"    " + this.arestaExtensaoMETROS.get(x) + "m\n"
                        + node_lat[fromNodI.get(x)] + ", " + node_lon[fromNodI.get(x)] + "\n" + node_lat[toNodI.get(x)] + ", " + node_lon[toNodI.get(x)]);

                for (int w = 0; w < this.getWayCount(); w++) {
                    if (way_id[w].equals(aresta.split("#")[0])) {

                        for (int a = 0; a < way_nodes[w].size(); a++) {
                            int in = this.getNodeIndex(way_nodes[w].get(a));
                            System.out.println(node_lat[in] + ", " + node_lon[in]);
                            cods = cods + way_nodes[w].get(a) + "\n";

                        }

                    }
                }

                System.out.println(cods);
                return;
            }
        }
        System.out.println("Aresta " + aresta + " não encontrada!");

    }

    
    private double calcCoefAngularAresta(int aresta){
       return calcCoefAngularNodes(fromNodI.get(aresta),toNodI.get(aresta));
    }
    
    private double calcCoefAngularNodes(int node1, int node2){
        return calcCoefAngularPontos(node_lat[node1],node_lon[node1],node_lat[node2],node_lon[node2]);
    }
    
    private double calcCoefAngularPontos(double x1, double y1, double x2, double y2){
     return (y1-y2)/ (x1-x2);  //m = (y1 - y2)/(x1 - x2);
    }
    
    private double calcCoefLinear(double coefAngular, int node){
    //b = Y + m*X
    // coefLinear = node_lon[noFrom] + coefAngular * node_lat[noFrom];
    // EQ RETA: y =  coefAngular*x + coefLinear   //   y = m*x + b
        return node_lon[node] + coefAngular * node_lat[node];
    }
    
    private double calcCoefLinear(double coefAngular, double lat, double lon){
    //b = Y + m*X
    // coefLinear = node_lon[noFrom] + coefAngular * node_lat[noFrom];
    // EQ RETA: y =  coefAngular*x + coefLinear   //   y = m*x + b
        return lon + coefAngular * lat;
    }
    
    public ArrayList<Integer> getArestasVizinhasAteProfundidade(int arestaInicial, int profundidade){
    
     ArrayList<Integer> arestasPartida = new ArrayList<>();
     ArrayList<Integer> arestasFinal = new ArrayList<>();
     ArrayList<Integer> aux = new ArrayList<>();
     int prof = 0;
     int v;
     
     arestasPartida.add(arestaInicial);
     
     while(prof < profundidade){
     v = arestasPartida.size();
         
     for(int x=0; x<v;x++){
         aux = this.vizinhosArestaDaAresta[arestasPartida.get(x)];
     
         for(int x2=0; x2<aux.size();x2++)
             if(!arestasPartida.contains(aux.get(x2)))
                 arestasPartida.add(aux.get(x2));
         
     }
     
     for(int x3=0; x3<arestasPartida.size();x3++)
             if(!arestasFinal.contains(arestasPartida.get(x3)))
                 arestasFinal.add(arestasPartida.get(x3));
     
     aux = new ArrayList<>();
     for(int x=v; x<arestasPartida.size();x++)
         aux.add(arestasPartida.get(x));
     arestasPartida = aux;
     
     
     prof++;
     }
     
     
    return arestasFinal;
    }
    
    
    
    public int getIndexArestaProxima(double lat, double lon, double metrosDist, int arestaAnterior, double nextLat, double nextLon, boolean ahead, int profundidadeBusca) {
        double menorDist = 99999.0;
        double dist;
        int indexProx = -1;
        

        double coefLinear_inv;
        double x_intersec;
        double y_intersec;
        double distNext = 999999;
        double distProx;

        int noFrom;
        int noTo;
        int aresta;
                       
        if((arestaAnterior != -1)){
        //definir conjunto de "arestas iniciais"
        ArrayList<Integer> arestasPartida = new ArrayList<>();
        arestasPartida.addAll(getArestasVizinhasAteProfundidade(arestaAnterior,profundidadeBusca));//arestasPartida.addAll(vizinhosArestaDaAresta[arestaAnterior]);
        int contProfundidade=0;
        int contAresta=0;
        /*int cont=0;
        for(int contProfundidade=0;contProfundidade<profundidadeBusca;contProfundidade++){
            for(;cont<arestasPartida.size();cont++)
                arestasPartida.addAll(vizinhosArestaDaAresta[arestasPartida.get(cont)]);
        }*/
        
      
        
        while (arestaAnterior != -1) {
            //ArrayList<Integer> vizinhos = this.getVizinhosIndexDoNode(toNodI.get(arestaAnterior), false);
            noFrom = toNodI.get(arestaAnterior);
            //primeiro procura nos vizinhos da aresta anterior
            //for (int v = 0; v < vizinhosIndex[toNodI.get(arestaAnterior)].size(); v++) {
            for (int v = 0; v <  vizinhosArestaDaAresta[arestaAnterior].size(); v++) {

                //noTo = vizinhos.get(v);
                aresta =  vizinhosArestaDaAresta[arestaAnterior].get(v); //  this.vizinhosNodeAresta[arestaAnterior].get(v);
                noTo = toNodI.get(aresta);

                if(isPointNearAresta(aresta, lat, lon)){//if (betweenNodes(lat, lon, noFrom, noTo, (2.0 * 0.000008))) {
                    //m = (y1 - y2)/(x1 - x2);
                    //coefAngular = (node_lon[noFrom] - node_lon[noTo]) / (node_lat[noFrom] - node_lat[noTo]);
                    //b = Y + m*X
                    //coefLinear = node_lon[noFrom] + coefAngular * node_lat[noFrom];
                    //EQ RETA: y =  coefAngular*x + coefLinear   //   y = m*x + b

                    coefLinear_inv = lon + arestaCoefAngular.get(aresta) * lat;
                    //EQ RETA = y = coefAngular_inv*x + coefLinear_inv

                    //ponto intersec:   0 = (coefAngular-coefAngular_inv)*x + (coefLinear-coefLinear_inv);
                    //ponto intersec:   -(coefLinear-coefLinear_inv) = (coefAngular-coefAngular_inv)*x
                    //ponto_intersec:   x  =  -(coefLinear-coefLinear_inv)/(coefAngular-coefAngular_inv)
                    x_intersec = ((-arestaCoefLinear.get(aresta) + coefLinear_inv) / (arestaCoefAngular.get(aresta) - arestaCoefAngularInv.get(aresta)));
                    y_intersec = arestaCoefAngularInv.get(aresta) * x_intersec + coefLinear_inv;

                    dist = (distanciaPontosMETROS(-x_intersec, y_intersec, lat, lon));
                    //System.out.println("lat "+lat+" lon "+lon+"; Aresta "+ar+"; intersec: "+x_intersec+" "+y_intersec+" dist "+dist+"m ");
                    if (/*dist < menorDist && */dist < metrosDist) {

                        if (nextLat != -1 && ahead) {
                            if (indexProx == -1 || Math.random() > 0.5) {
                                indexProx = getIndexArestaProxima(nextLat, nextLon, metrosDist, getArestaIndex(noFrom, noTo), -1, -1, false,profundidadeBusca);
                            }

                        } else {

                            distProx = (distanciaPontosMETROS(node_lat[noTo], node_lon[noTo], nextLat, nextLon) / 0.0000089);
                            if (distProx < distNext) { //só troca se o proximo ponto estiver se mais proximo da ponta de saída da aresta 
                                menorDist = dist;   //ou seja: Ou está dentro da aresta (sentido da aresta e chegando perto), ou já saiu da aresta depois, e também fica mais perto                        
                                indexProx = getArestaIndex(noFrom, noTo);
                                distNext = distProx;
                            }
                        }

                    }

                }
            }
            
        if(arestasPartida.size()>0 && contAresta < arestasPartida.size()-1){
            arestaAnterior = arestasPartida.get(contAresta);
            contAresta++;
        }else    
            arestaAnterior = -1;
            
        }//fim while
        
            
        }//fim if arestaValida

        
        
        //se não tiver encontrado nada nos vizinhos, vai para a busca geral
        if (indexProx == -1) {
            for (int ar = 0; ar < fromNodI.size(); ar++) { //passando por todas as arestas
                if(isPointNearAresta(ar, lat, lon)){//if (betweenNodes(lat, lon, fromNodI.get(ar), toNodI.get(ar), (2.0 * 0.0000089))) {
                    //m = (y1 - y2)/(x1 - x2);
                    //coefAngular = (node_lon[fromNodI.get(ar)] - node_lon[toNodI.get(ar)]) / (node_lat[fromNodI.get(ar)] - node_lat[toNodI.get(ar)]);
                    //b = Y + m*X
                    //coefLinear = node_lon[fromNodI.get(ar)] + coefAngular * node_lat[fromNodI.get(ar)];
                    //EQ RETA: y =  coefAngular*x + coefLinear   //   y = m*x + b

                    coefLinear_inv = lon + arestaCoefAngularInv.get(ar) * lat;
                    //EQ RETA = y = coefAngular_inv*x + coefLinear_inv

                    //ponto intersec:   0 = (coefAngular-coefAngular_inv)*x + (coefLinear-coefLinear_inv);
                    //ponto intersec:   -(coefLinear-coefLinear_inv) = (coefAngular-coefAngular_inv)*x
                    //ponto_intersec:   x  =  -(coefLinear-coefLinear_inv)/(coefAngular-coefAngular_inv)
                    x_intersec = ((-arestaCoefLinear.get(ar) + coefLinear_inv) / (arestaCoefAngular.get(ar) - arestaCoefAngularInv.get(ar)));
                    y_intersec = arestaCoefAngularInv.get(ar) * x_intersec + coefLinear_inv;

                    dist = (distanciaPontosMETROS(-x_intersec, y_intersec, lat, lon));
                    //System.out.println("lat "+lat+" lon "+lon+"; Aresta "+ar+"; intersec: "+x_intersec+" "+y_intersec+" dist "+dist+"m ");
                    if (/*dist < menorDist && */dist < metrosDist) {

                        if (nextLat != -1 && ahead) {
                            if (indexProx == -1 || Math.random() > 0.5) {
                                indexProx = getIndexArestaProxima(nextLat, nextLon, metrosDist, ar, -1, -1, false,profundidadeBusca-1);
                            }
                        } else {

                            distProx = (distanciaPontosMETROS(node_lat[toNodI.get(ar)], node_lon[toNodI.get(ar)], nextLat, nextLon) / 0.0000089);
                            if ((distProx < distNext) || nextLat < 0) { //só troca se o proximo ponto estiver se mais proximo da ponta de saída da aresta 
                                //ou seja: Ou está dentro da aresta (sentido da aresta e chegando perto), ou já saiu da aresta depois, e também fica mais perto                        
                                menorDist = dist;
                                indexProx = ar;
                                distNext = distProx;
                            }
                        }

                    }

                }
            }
        }
        return indexProx;
    }

    public boolean betweenNodes(double lat, double lon, int node1, int node2, double folga) {

        if (between(lat, node_lat[node1], node_lat[node2], folga)) {
            return (between(lon, node_lon[node1], node_lon[node2], folga));
        } else {
            return false;
        }
    }

    public boolean between(double meio, double ponta1, double ponta2, double folga) {

        if (mod(ponta1, ponta2) < (folga)) {
            folga = folga * 2;
        }

        if (meio >= (ponta1 - folga)) {
            return meio <= (ponta2 + folga);
        } else if (meio >= (ponta2 - folga)) {
            return (meio <= (ponta1 + folga));
        } else {
            return false;
        }
    }
    
    public boolean between(double meio, double ponta1, double ponta2) {

        if (meio >= (ponta1)) {
            return meio <= (ponta2);
        } else if (meio >= (ponta2)) {
            return (meio <= (ponta1));
        } else {
            return false;
        }
    }
    

    public void criarArestasDirecionadas(String arquivoSUMO) {

        temArestas = true;

        fromNod = new ArrayList<>();
        toNod = new ArrayList<>();
        fromNodI = new ArrayList<>();
        toNodI = new ArrayList<>();
        arestaKind = new ArrayList<>();
        arestaCodSUMO = new ArrayList<>();
        arestaExtensaoMETROS = new ArrayList<>();
        arestaExtensaoGRAUS = new ArrayList<>();
        arestaCoefAngular = new ArrayList<>();
        arestaCoefAngularInv  = new ArrayList<>();
        arestaCoefLinear  = new ArrayList<>();

        if (arquivoSUMO.length() < 5) {
            //de way em way
            System.out.println("PROC: Cadastrando arestas...  (Mapping, OSM File) " + horaAtual());
            for (int w = 0; w < getWayCount(); w++) {
                //adiciona arestas deste way

                addArestasDoWay(w, way_nodes[w], oneWay[w], way_kind[w]);
            }

        } else {
            System.out.println("PROC: Cadastrando arestas...  (Mapping, SUMO XML) " + horaAtual());
            this.lerEdgesXMLSumo(arquivoSUMO);

        }

        //indiceArestKind = new int[fromNod.size()];
        //contAresta = new int[getFromNodCod().size()][discretTemporal + 1];
        //contArestaBatch = new short[getFromNodCod().size()][discretTemporal + 1][batchSize];
        //arestaVariance = new double[getFromNodCod().size()][discretTemporal + 1];
        //contagens não importam para o objeto Mapping
        System.out.println("OK: Cadastrou " + getFromNod().size() + " arestas! ");

    }

    private void addArestasDoWay(int numWay, ArrayList<String> way_node, boolean oneWay, String kind) {

        for (int x = 0; x < way_node.size() - 1; x++) {

            fromNod.add(way_node.get(x));
            toNod.add(way_node.get(x + 1));
            arestaKind.add(kind);
            fromNodI.add(getNodeIndex(way_node.get(x)));
            toNodI.add(getNodeIndex(way_node.get(x + 1)));
            arestaCoefAngular.add(calcCoefAngularAresta(fromNodI.size()-1));
            arestaCoefLinear.add(calcCoefLinear(arestaCoefAngular.get(fromNodI.size()-1), getNodeIndex(way_node.get(x))));
            arestaCoefAngularInv.add(1/arestaCoefAngular.get(arestaCoefAngular.size()-1));
            arestaCodSUMO.add(way_id[numWay] + "#" + x);
            arestaExtensaoMETROS.add(distanciaMNodes(way_node.get(x), way_node.get(x + 1)));
            arestaExtensaoGRAUS.add(distanciaNodesGRAUS(way_node.get(x), way_node.get(x + 1)));

            if (!oneWay) {

                fromNod.add(way_node.get(x + 1));
                toNod.add(way_node.get(x));
                arestaKind.add(kind);
                fromNodI.add(getNodeIndex(way_node.get(x + 1)));
                toNodI.add(getNodeIndex(way_node.get(x)));
                arestaCoefAngular.add(calcCoefAngularAresta(fromNodI.size()-1));
                arestaCoefLinear.add(calcCoefLinear(arestaCoefAngular.get(fromNodI.size()-1), getNodeIndex(way_node.get(x))));
                arestaCoefAngularInv.add(1/arestaCoefAngular.get(arestaCoefAngular.size()-1));
                arestaCodSUMO.add("-" + way_id[numWay] + "#" + x);
                arestaExtensaoMETROS.add(distanciaMNodes(way_node.get(x), way_node.get(x + 1)));
                arestaExtensaoGRAUS.add(distanciaNodesGRAUS(way_node.get(x), way_node.get(x + 1)));

            }

        }

    }

    public ArrayList<String> getArestaKindVet() {
        return arestaKind;
    }

    public String identificarNodeMaisProximoCodigo(double lat, double lon, double metrosRaioNode, double raioParaRandom) {
        int x = identificarNodeMaisProximoIndex(lat, lon, metrosRaioNode, raioParaRandom);
        if (x != -1) {
            return node_id[x];
        } else {
            System.out.println("ERROR: Nao achou node proximo (Mapping.identificarNodeMaisProximoCodigo)");
            return "FALHA";
        }
    }

    public int identificarNodeMaisProximoIndex(double lat, double lon, double metrosRaioNode, double raioParaRandom) {

        ArrayList<Integer> nodesCand = new ArrayList<>();

        for (int y = 0; y < contNodes; y++) {
            if (mod(node_lat[y], lat) < (((double) metrosRaioNode) * 0.0000089)) {
                if (mod(node_lon[y], lon) < (((double) metrosRaioNode) * 0.0000089)) {
                    nodesCand.add(y);
                }
            }

        }//acabou laço de analisar MAPnodes

        if (!nodesCand.isEmpty()) {      //se há nodes candidatos para a posição registada X          
            //encontra node que é mais próximo
            double menorDist = this.distanciaPontosMETROS(lat, lon, node_lat[nodesCand.get(0)], node_lon[nodesCand.get(0)]);
            int indexMaisProx = nodesCand.get(0);
            double aux;

            for (int a = 1; a < nodesCand.size(); a++) {
                aux = distanciaPontosMETROS(lat, lon, node_lat[nodesCand.get(a)], node_lon[nodesCand.get(a)]);
                if (aux < menorDist && aux < metrosRaioNode) {
                    indexMaisProx = nodesCand.get(a);
                    menorDist = aux;
                } else if (aux < raioParaRandom && Math.random() > 0.3) {
                    indexMaisProx = nodesCand.get(a);
                    menorDist = aux;
                }
            }
            //retorna node mais próximo    
            return indexMaisProx;
        }
        //se não houver
        //System.out.println("FAIL: Buscou em "+contNodes+" nodes, com "+metrosRaioNode+"m de raio, sem correspondente!");
        return -1;
    }

    public String getCodArestaMaisProxSUMO(double lat, double lon, double metrosRaioNode) {

        //int index = identificarArestaMaisProximoIndex(lat, lon, metrosRaioNode);
        int index = getIndexArestaProxima(lat, lon, metrosRaioNode, -1, lat, lon, false,0);

        if (index != -1) {
            return this.arestaCodSUMO.get(index);//return this.getSUMOEdgeCode(fromNodI.get(index), toNodI.get(index));
        } else {
            return "";
        }

    }

    public int identificarArestaMaisProximoIndex(double lat, double lon, double metrosRaioNode) {

        ArrayList<Double> latx = new ArrayList<>();
        ArrayList<Double> lonx = new ArrayList<>();
        ArrayList<Integer> indexAresta = new ArrayList<>();

        for (int a = 0; a < fromNodI.size(); a++) {
            //centro
            latx.add((node_lat[fromNodI.get(a)] + node_lat[toNodI.get(a)]) / 2);
            lonx.add((node_lon[fromNodI.get(a)] + node_lon[toNodI.get(a)]) / 2);
            indexAresta.add(a);
            //lateral 1
            latx.add((latx.get(latx.size() - 1) + node_lat[toNodI.get(a)]) / 2);
            lonx.add((lonx.get(lonx.size() - 1) + node_lon[toNodI.get(a)]) / 2);
            indexAresta.add(a);
            //lateral 2
            latx.add((node_lat[fromNodI.get(a)] + latx.get(latx.size() - 2)) / 2);
            lonx.add((node_lon[fromNodI.get(a)] + lonx.get(latx.size() - 2)) / 2);
            indexAresta.add(a);
        }

        ArrayList<Integer> ArestaCand = new ArrayList<>();
        ArrayList<Integer> indexLatLon = new ArrayList<>();

        for (int y = 0; y < indexAresta.size(); y++) {
            if (mod(latx.get(y), lat) < (((double) metrosRaioNode) * 0.0000089)) {
                if (mod(lonx.get(y), lon) < (((double) metrosRaioNode) * 0.0000089)) {
                    ArestaCand.add(indexAresta.get(y));
                    indexLatLon.add(y);
                }
            }

        }//acabou laço de analisar MAPnodes

        if (!ArestaCand.isEmpty()) {      //se há nodes candidatos para a posição registada X          
            //encontra node que é mais próximo
            double menorDist = distanciaPontosMETROS(lat, lon, latx.get(indexLatLon.get(0)), lonx.get(indexLatLon.get(0)));
            int indexMaisProx = indexLatLon.get(0);
            double aux;

            for (int a = 1; a < ArestaCand.size(); a++) {
                aux = distanciaPontosMETROS(lat, lon, latx.get(indexLatLon.get(0)), lonx.get(indexLatLon.get(0)));
                if (aux < menorDist) {
                    indexMaisProx = indexLatLon.get(a);
                    menorDist = aux;
                }
            }
            //retorna node mais próximo    
            return indexAresta.get(indexMaisProx);
        }
        //se não houver
        return -1;
    }

    /*public String getSUMOEdgeCode(String node1, String node2){ 
        for(int w=0;w<way_id.length;w++)
            if(way_nodes[w].contains(node1))
                for(int n=0;n<way_nodes[w].size()-1;n++)
                    if(way_nodes[w].get(n).equals(node1))
                        if(way_nodes[w].get(n+1).equals(node2))
                            return way_id[w]+"#"+n;
        return "FALHA";
    } */
    public void encontrarNodesGemeos(double metrosMax) {

        int maxGem = 14;

        System.out.println("PROC: Encontrando nodes gemeos... " + horaAtual());
        int cont = 0;
        gemeosIndex = new int[contNodes][maxGem];
        contGemeos = new int[contNodes];

        for (int x = 0; x < contNodes; x++) {
            contGemeos[x] = 0;
            for (int y = 0; y < contNodes; y++) {

                if (x != y) {  //não sendo o mesmo nó

                    if (distanciaMNodes(x, y) <= metrosMax) {

                        gemeosIndex[x][contGemeos[x]] = y;
                        gemeosIndex[y][contGemeos[y]] = x;

                        contGemeos[x]++;
                        contGemeos[y]++;
                        cont++;

                        if (contGemeos[x] == maxGem || contGemeos[y] == maxGem) { //ocupou a última posição do vetor
                            maxGem++; //mais um espaço para a direita

                            int[][] gemeosIndex2;
                            gemeosIndex2 = new int[contNodes][maxGem];

                            for (int a = 0; a < contNodes; a++) {
                                for (int b = 0; b < contGemeos[b]; b++) {
                                    gemeosIndex2[a][b] = gemeosIndex[a][b];
                                }
                            }

                            gemeosIndex = gemeosIndex2;

                        }

                    }

                }

            }
        }

        System.out.println("OK: Encontrou " + cont + "  nodes gemeos! (" + metrosMax + "m) " + horaAtual());
    }

    public double distanciaMNodes(int n1, int n2) {

        return distanciaPontosMETROS(node_lat[n1], node_lon[n1], node_lat[n2], node_lon[n2]);

    }

    public double distanciaMNodes(String no1, String no2) {

        int n1, n2;
        n1 = this.getNodeIndex(no1);
        n2 = this.getNodeIndex(no2);

        return distanciaPontosMETROS(node_lat[n1], node_lon[n1], node_lat[n2], node_lon[n2]);

    }



    public double distanciaNodesGRAUS(String no1, String no2) {

        int n1, n2;
        n1 = this.getNodeIndex(no1);
        n2 = this.getNodeIndex(no2);

        return distanciaPontosGRAUS(node_lat[n1], node_lon[n1], node_lat[n2], node_lon[n2]);

    }
    
    public double distanciaNodesGRAUS(int n1, int n2) {

        return distanciaPontosGRAUS(node_lat[n1], node_lon[n1], node_lat[n2], node_lon[n2]);

    }
    
    
    public double distanciaPontosGRAUS(double x1, double y1, double x2, double y2){
        return DirectDistance(x1, y1, x2, y2) * 0.0000089;
    }
    
    public double distanciaPontosMETROS(double x1, double y1, double x2, double y2) {

        //return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)) / 0.0000089;
        return DirectDistance(x1, y1, x2, y2);

    }
    
    double LOCAL_PI = 3.1415926535897932385;

    double ToRadians(double degrees) {
        double radians = degrees * LOCAL_PI / 180;
        return radians;
    }

    double DirectDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75;
        double dLat = ToRadians(lat2 - lat1);
        double dLng = ToRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(ToRadians(lat1)) * Math.cos(ToRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;
        double meterConversion = 1609.00;
        return dist * meterConversion;
    }

    ArrayList<Integer>[] vizinhosDaAresta;
    ArrayList<Integer>[] vizinhosArestaDaAresta;

    public ArrayList<Integer> getVizinhosNodesDaAresta(int aresta, int noPonta) {
        if (aresta == -1 || vizinhosDaAresta.length < 1) {
            return vizinhosIndex[noPonta];

        } else {
            return vizinhosDaAresta[aresta];
        }
    }

    public ArrayList<Integer> getVizinhosArestasDaAresta(int aresta, int noPonta) {
        if (aresta == -1 || vizinhosDaAresta.length < 1) {
            return this.vizinhosNodeAresta[noPonta];//this.vizinhosNodeAresta[noPonta];//   getVizinhosIndexDoNode(noPonta, false);

        } else {
            return vizinhosArestaDaAresta[aresta];
        }
    }

    public void cadastrarVizinhosArestas() {
        vizinhosDaAresta = new ArrayList[fromNodI.size()];
        vizinhosArestaDaAresta = new ArrayList[fromNodI.size()];

        for (int ar = 0; ar < fromNodI.size(); ar++) {
            vizinhosDaAresta[ar] = calcVizinhosPermitidosPorIndexDeAresta(ar);
            vizinhosArestaDaAresta[ar] = calcVizinhosArestasPermitidasPorIndexDeAresta(ar);
        }
    }

    public ArrayList<Integer> calcVizinhosArestasPermitidasPorIndexDeAresta(int aresta) {
        ArrayList<Integer> viz = new ArrayList<>();
        int nodePonta = toNodI.get(aresta);
        ArrayList<Integer> v = vizinhosNodeAresta[nodePonta];

        if (aresta == -1 || connectionFromIndex.isEmpty()) {

            for (int va = 0; va < v.size(); va++) {
                viz.add(v.get(va));
            }

        } else {

            for (int va = 0; va < v.size(); va++) {
                if (this.existeConnectionEntreArestas(aresta, vizinhosNodeAresta[nodePonta].get(va))) {
                    viz.add(v.get(va));
                }
            }

        }

        return viz;
    }

    public ArrayList<Integer> calcVizinhosPermitidosPorIndexDeAresta(int aresta) {
        ArrayList<Integer> viz = new ArrayList<>();
        int nodePonta = toNodI.get(aresta);
        ArrayList<Integer> v = this.vizinhosIndex[nodePonta]; //getVizinhosIndexDoNode(nodePonta,false);

        if (aresta == -1 || connectionFromIndex.isEmpty()) {

            for (int va = 0; va < v.size(); va++) {
                viz.add(v.get(va));
            }

        } else {

            for (int va = 0; va < v.size(); va++) {
                if (this.existeConnectionEntreArestas(aresta, vizinhosNodeAresta[nodePonta].get(va))) {
                    viz.add(v.get(va));
                }
            }

        }

        return viz;
    }

    public ArrayList<Integer> getVizinhosIndexDoNode(int index, boolean usarGemeos) {

        if (!usarGemeos || contGemeos[index] == 0) {
            return vizinhosIndex[index];
        } else {
            ArrayList<Integer> ars = vizinhosIndex[index];

            for (int x = 0; x < contGemeos[index]; x++) {
                ars.add(gemeosIndex[index][x]);
            }

            return ars;

        }
    }

    public int getContVizinhosNode(int index, boolean usarGemeos) {

        if (!usarGemeos) {
            return vizinhosIndex[index].size();
        } else {
            return (vizinhosIndex[index].size() + contGemeos[index]);
        }
    }

    /* public String[] getVizinhosCodDoNode(int index) {
        return vizinhosCod[index];
    }*/
    public int getNodeIndex(String nod) {

        for (int a = 0; a < contNodes; a++) {
            if (nod.equals(node_id[a])) {
                return a;
            }
        }
        
        if(contNodes<10)
            System.out.println("ERROR: contNodes = "+contNodes+" (Mapping)");
        
        return -1;
    }

    public ArrayList<Integer> getVizinhosNodeArestasIndex(int node) {
        return vizinhosNodeAresta[node];
    }

    public void cadastrarVizinhos() {

        if (vizinhosCadastrados) {
            System.out.println("INFO: Vizinhos já cadastrados anteriormente no objeto map.");
            return;
        }

        System.out.println("PROC: Cadastrando vizinhos dos nós... " + horaAtual());

        int totalVizinhos = 0;
        vizinhosIndex = new ArrayList[contNodes];
        vizinhosNodeAresta = new ArrayList[contNodes];
        vizinhosCod = new ArrayList[contNodes];

        //de nó em nó
        for (int x = 0; x < contNodes; x++) {

            vizinhosIndex[x] = new ArrayList<>();
            vizinhosCod[x] = new ArrayList<>();
            vizinhosNodeAresta[x] = new ArrayList<>();

            //de aresta em aresta
            for (int a = 0; a < fromNodI.size(); a++) {

                if (fromNodI.get(a) == x) { // se fromNodIndex é do nó em questão

                    vizinhosIndex[x].add(toNodI.get(a)); //toNodIndex é um vizinho
                    vizinhosCod[x].add(node_id[toNodI.get(a)]); //toNodCod é um vizinho também
                    vizinhosNodeAresta[x].add(a);
                    totalVizinhos++;
                }

            }

        }

        System.out.println("OK: Cadastrou " + totalVizinhos + " vizinhos dos nós, a partir de " + this.getContArestas() + " arestas! " + horaAtual());

        cadastrarVizinhosArestas();

        vizinhosCadastrados = true;
    }

    public void comp(String s, String s1) {
        System.out.println("Comparando '" + s + "' e '" + s1 + "'");
    }

    public boolean wayDoNodeEhOneWay(int indexNode) {

        for (int a = 0; a < wayCount; a++) {
            for (int c = 0; c < way_nodes[a].size(); c++) {
                if (way_nodes[a].get(c).equals(node_id[indexNode])) {
                    return oneWay[a];
                }
            }
        }

        return false;
    }

    public Mapping(int roiSize, String arquivoSUMO) {

        geradordeRede2 gerador = new geradordeRede2();

        ROIsizeM = roiSize;
        int nodes = 0;
        int ways = 0;
        int aux = 0;
        int med = 0, maxNodes = -1, minNodes = -1;

        //contando nodes do arquivo
        nodes = gerador.getContNodes();
        System.out.println(nodes + " nodes, " + gerador.getContArestas() + " arestas.");

        //Inicia processamento de nodes
        //laço de criação de nodes
        node_id = new String[nodes];
        node_lat = new Double[nodes];
        node_lon = new Double[nodes];
        contNodes = 0;

        for (; contNodes < nodes; contNodes++) {

            node_id[contNodes] = gerador.getCodNo().get(contNodes);
            node_lat[contNodes] = gerador.getLatNo().get(contNodes);
            node_lon[contNodes] = gerador.getLonNo().get(contNodes);

        }

        // System.out.println("\nIniciando processamento de ways...");
        //criação dos vetores
        way_id = new String[gerador.getContArestas()];
        way_nodes = new ArrayList[gerador.getContArestas()]; //way_nodes
        way_kind = new String[gerador.getContArestas()];

        for (int a = 0; a < (int) (ways * fatorRedux); a++) {
            way_nodes[a] = new ArrayList<>();
        }
        oneWay = new boolean[gerador.getContArestas()];

        //identifica começo e fim do <way> </way>
        int wayEmProcesso = 0;
        for (; wayEmProcesso < gerador.getContArestas(); wayEmProcesso++) {
            way_id[wayEmProcesso] = "w" + wayEmProcesso;
            way_kind[wayEmProcesso] = "" + gerador.getTipoLink().get(wayEmProcesso);
            way_nodes[wayEmProcesso] = new ArrayList<>();
            way_nodes[wayEmProcesso].add(gerador.getFromNoC().get(wayEmProcesso));
            way_nodes[wayEmProcesso].add(gerador.getToNoC().get(wayEmProcesso));
            oneWay[wayEmProcesso] = true;
        }

        wayCount = wayEmProcesso - 1;

        System.out.println("Utilizou " + wayEmProcesso + " / " + gerador.getContArestas() + " espaços do vetor de WAYS.");

        //encontrar nodes opostos (contNodes, node_lat)
        menorLat = node_lat[0];
        maiorLat = node_lat[0];
        menorLon = node_lon[0];
        maiorLon = node_lon[0];

        for (int i = 1; i < contNodes; i++) {
            if (node_lat[i] < getMenorLat()) {
                menorLat = node_lat[i];
            } else if (node_lat[i] > getMaiorLat()) {
                maiorLat = node_lat[i];
            }
            if (node_lon[i] < getMenorLon()) {
                menorLon = node_lon[i];
            } else if (node_lon[i] > getMaiorLon()) {
                maiorLon = node_lon[i];
            }
        }

        criarArestasDirecionadas(arquivoSUMO);
        
        //this.showStatsTamanhoArestas();
    }

    public boolean ispontoDentroDoMapa(double lat, double lon) {

        if (between(lat, this.maiorLat, this.menorLat, 0.0)) {
            if (between(lon, this.maiorLon, this.menorLon, 0.0)) {
                return true;
            }
        }
        return false;
    }

    ArrayList<String> hwaykinds;

    int linhaInicial = 0;
    int linhaFinal = 1000000;
    String[] tempLinhas;
    String OSMFileLocation;

    private String getLine(int linha) {
     if(linha<=linhaInicial)
         load(linha);
        
        if (linha < linhaFinal) {
            return tempLinhas[linha - linhaInicial];
        } else {
            load(linha);
        }
        return tempLinhas[linha - linhaInicial];
    }

    private void load(int linhaIn) {
        linhaFinal = linhaIn + (linhaFinal - linhaInicial);
        linhaInicial = linhaIn;
        tempLinhas = new String[(linhaFinal - linhaInicial)];
        String aux;
        try {
            Scanner scanner = new Scanner(new File(OSMFileLocation));
            scanner.useDelimiter("\n");
            int c = 0;
            while (c < (linhaFinal ) && c < totalLinhas) {

                aux = scanner.next();
                if (c >= linhaInicial) {
                    tempLinhas[c - linhaInicial] = aux;
                }
                c++;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private int contLinhasFile(String file) {
        int c = 0;
        try {
            Scanner scanner = new Scanner(new File(file));
            scanner.useDelimiter("\n");

            while (scanner.hasNext()) {
                scanner.next();
                c++;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }

    int totalLinhas;
    
    public Mapping(String osmFile, boolean calc, int roiSize, double metrosMaxParaSerNodeGemeo, String arquivoSUMO) {

        OSMFileLocation = osmFile;
        calcWaysPorNode = calc;
        ROIsizeM = roiSize;
        totalLinhas = contLinhasFile(osmFile);
        
        
        String[] sep;
        int nodes = 0;
        int detalhesNodes = 0;
        int ways = 0;
        int detalhesWays = 0;
        int aux = 0;
        int linhasInfo = 0;
        int med = 0, maxNodes = -1, minNodes = -1;

        //String[] linhas = s.split("\n");
        //s = "";
        System.out.println("Total de " + totalLinhas + " linhas no arquivo OSM." + osmFile);

        for (int x = 0; x < totalLinhas; x++) {
            if (getLine(x).length() > 3) {
                // comp(linhas[x].substring(0, 4).replace(" ", ""),"<node".substring(0, 4));
                if (getLine(x).replace(" ", "").substring(0, 4).equals("<node".substring(0, 4))) {
                    linhasInfo = x;
                    x = totalLinhas;
                }
            }
        }
        indexNodes = linhasInfo;

        //contando nodes do arquivo
        for (int x = linhasInfo; x < totalLinhas; x++) {
            if (getLine(x).length() > 3) {
                if (getLine(x).replace(" ", "").substring(0, 4).equals("<way".substring(0, 4))) {
                    nodes = x - linhasInfo - detalhesNodes;
                    aux = x;
                    x = totalLinhas;
                } else if (!getLine(x).replace(" ", "").substring(0, 3).equals("<node".substring(0, 3))) {
                    detalhesNodes++;
                }
            } else {
                detalhesNodes++;
            }
        }
        indexWays = aux;

        //contando ways do arquivo
        for (; aux < totalLinhas; aux++) {
            if (getLine(aux).length() > 3) {
                if (getLine(aux).replace(" ", "").substring(0, 3).equals("<relation".substring(0, 3))) {
                    ways = aux - linhasInfo - nodes - detalhesNodes - detalhesWays;
                    aux = totalLinhas;
                } else if (!getLine(aux).replace(" ", "").substring(0, 3).equals("<way".substring(0, 3))) {
                    detalhesWays++;
                }
            } else {
                detalhesWays++;
            }
        }
        indexEnd = indexWays + detalhesWays + ways;

        System.out.println(nodes + " nodes, " + detalhesNodes + " detalhes de nodes. " + ways + " ways, " + detalhesWays + " detalhes de ways.");

        //Inicia processamento de nodes
        //descobre em que índices os valores estão
        int posId = -1, posLat = -1, posLon = -1;

        System.out.println("Usando como modelo a linha: " + getLine(indexNodes));
        sep = getLine(indexNodes).split("\"");
        for (int a = 0; a < sep.length; a++) {
            if (sep[a].replace(" ", "").equals("<nodeid=")) {
                posId = a + 1;
            } else if (sep[a].replace(" ", "").equals("lat=")) {
                posLat = a + 1;
            } else if (sep[a].replace(" ", "").equals("lon=")) {
                posLon = a + 1;
            }// else if (sep[a].replace(" ", "").equals("visible=")) {
            //      posVisible = a + 1;
            //  }
        }
        System.out.println("posID = " + posId + "; posLat = " + posLat + "; posLon = " + posLon + ";");

        //laço de criação de nodes
        node_id = new String[nodes];
        node_lat = new Double[nodes];
        node_lon = new Double[nodes];
        for (int a = indexNodes; a < indexWays; a++) {
            if (getLine(a).length() > 3) {
                if (getLine(a).replace(" ", "").substring(0, 4).equals("<node".substring(0, 4))) {
                    sep = getLine(a).split("\"");
                    //System.out.println("node visible: "+sep[posVisible]);
                    //  if (sep[posVisible].equals("true")) {
                    node_id[contNodes] = sep[posId].replace(" ", "");
                    node_lat[contNodes] = Double.valueOf(sep[posLat]);
                    node_lon[contNodes] = Double.valueOf(sep[posLon]);

                    if (contNodes == 0) {
                        maiorLat = node_lat[contNodes];
                        menorLat = node_lat[contNodes];
                        maiorLon = node_lon[contNodes];
                        menorLon = node_lon[contNodes];
                    } else if (node_lat[contNodes] > maiorLat) {
                        maiorLat = node_lat[contNodes];
                    } else if (node_lat[contNodes] < menorLat) {
                        menorLat = node_lat[contNodes];
                    }

                    if (node_lon[contNodes] > maiorLon) {
                        maiorLon = node_lon[contNodes];
                    } else if (node_lon[contNodes] < menorLon) {
                        menorLon = node_lon[contNodes];
                    }

                    contNodes++;
                    //  System.out.print(sep[posId].replace(" ", "")+ " ");
                    //  }
                }
            }
        }

        // System.out.println("\nIniciando processamento de ways...");
        //Inicia processamento de ways 
        /*<way id="452139651" version="1" timestamp="2016-11-08T21:15:23Z" changeset="43498787" uid="4400624" user="Sílvio Matos">
        <nd ref="299584409"/>
        <nd ref="1731169402"/>
        <nd ref="1731169056"/>
        <tag k="highway" v="residential"/>
        <tag k="name" v="Rua Antero de Araújp"/>
        <tag k="oneway" v="yes"/>
        <tag k="source" v="Bing"/>
        </way>*/
        //criação dos vetores
        way_id = new String[(int) (ways * fatorRedux)];
        way_name = new String[(int) (ways * fatorRedux)];
        way_nodes = new ArrayList[(int) (ways * fatorRedux)]; //way_nodes
        way_kind = new String[(int) (ways * fatorRedux)];

        for (int a = 0; a < (int) (ways * fatorRedux); a++) {
            way_nodes[a] = new ArrayList<>();
        }
        oneWay = new boolean[(int) (ways * fatorRedux)];

        int inicioWay = indexWays, fimNdRef = -1, fimWay;
        int localTagHighway = 0, localTagOneWay = -1, localTagNome = -1;
        int wayEmProcesso = 0;

        hwaykinds = new ArrayList<>();
        hwaykinds.add("<tagk=\"highway\"v=\"pedestrian\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"track\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"escape\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"raceway\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"footway\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"bridleway\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"steps\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"path\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"cycleway\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"proposed\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"bus_stop\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"crossing\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"elevator\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"emergency_acess_point\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"give_way\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"speed_camera\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"street_lamp\"/>");
        // hwaykinds.add("<tagk=\"highway\"v=\"services\"/>");
        // hwaykinds.add("<tagk=\"highway\"v=\"service\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"stop\"/>");
        hwaykinds.add("<tagk=\"highway\"v=\"traffic_signals\"/>");

        int az = hwaykinds.size();
        for (int x = 0; x < az; x++) {
            hwaykinds.add(hwaykinds.get(x).split("\"")[3]);
        }

        int oneWayNaoIdentificados = 0;
        int highWayNaoIdentificados = 0;
        int nomeWayNaoIdentificados = 0;

        //identifica começo e fim do <way> </way>
        for (int x = indexWays; x < indexEnd; x = fimWay + 1) {

            inicioWay = x;
            fimWay = indexEnd;
            localTagHighway = -1;
            localTagOneWay = -1;
            localTagNome = -1;

            //encontrar posições
            for (int y = x + 1; y < fimWay; y++) {
                if (!getLine(y).replace(" ", "").substring(0, 3).equals("<ndref".substring(0, 3))) {
                    fimNdRef = y - 1;
                    y = fimWay; //sai do laço ao parar de encontrar <nd ref
                }
            }

            for (int y = fimNdRef; y < fimWay; y++) {
                if (getLine(y).replace(" ", "").substring(0, 4).equals("</way".substring(0, 4))) {
                    fimWay = y;//sai do laço. Já achou todas as posições    
                } else if (getLine(y).replace(" ", "").substring(0, 9).equals("<tagk=\"highway".substring(0, 9))) {
                    localTagHighway = y;
                } else if (getLine(y).replace(" ", "").substring(0, 11).equals("<tagk=\"oneway".substring(0, 11))) {
                    localTagOneWay = y;
                } else if (getLine(y).replace(" ", "").substring(0, 14).equals("<tagk=\"name\"v=\"".substring(0, 14))) {
                    localTagNome = y;
                }
            }

            //System.out.println("OK: localTagHighWay = "+ localTagHighway+"; localTagOneWay = "+localTagOneWay+"; fimWay = "+fimWay);
            //caso seja highway, salva
            if (localTagHighway > inicioWay) { //significa que achou tag de highway
                //System.out.println();
                if (!hwaykinds.contains(getLine(localTagHighway).replace(" ", ""))) {//

                    way_id[wayEmProcesso] = getLine(inicioWay).split("\"")[1];
                    //salva nodes que fazem parte
                    for (int a = inicioWay + 1; a <= fimNdRef; a++) {
                        way_nodes[wayEmProcesso].add(getLine(a).split("\"")[1].replace(" ", ""));

                    }
                    //indica se é oneway

                    if (localTagOneWay != -1) {
                        oneWay[wayEmProcesso] = getLine(localTagOneWay).replace(" ", "").equals("<tagk=\"oneway\"v=\"yes\"/>");
                    } else {
                        oneWayNaoIdentificados++;
                        oneWay[wayEmProcesso] = true;
                    }

                    if (localTagHighway != -1) {
                        way_kind[wayEmProcesso] = getLine(localTagHighway).replace(" ", "");
                    } else {
                        way_kind[wayEmProcesso] = "?";
                        highWayNaoIdentificados++;
                    }

                    //salva nome da rua
                    if (localTagNome != -1) {
                    way_name[wayEmProcesso] = getLine(localTagNome).split("\"")[3]; //.replace("<tag k=\"name\" v=\"", "").replace("\"/>", "");
                    } else {
                        way_name[wayEmProcesso] = "?";
                        nomeWayNaoIdentificados++;
                    }
                    
                    med = med + way_nodes[wayEmProcesso].size();

                    if (wayEmProcesso == 0) {
                        maxNodes = med;
                        minNodes = med;
                    } else if (way_nodes[wayEmProcesso].size() > maxNodes) {
                        maxNodes = way_nodes[wayEmProcesso].size();
                    } else if (way_nodes[wayEmProcesso].size() < minNodes) {
                        minNodes = way_nodes[wayEmProcesso].size();
                    }

                    wayEmProcesso++;

                }
            }

        }//fim do laço de way

        wayCount = wayEmProcesso - 1;
        tempLinhas = null;
        
        System.out.println("Utilizou " + wayEmProcesso + " / " + (int) (ways * fatorRedux) + " (Redux. Original = " + ways + ") espaços do vetor de WAYS.\n"
                + "Média de " + med / wayCount + " nodes por way. Max: " + maxNodes + "; Min: " + minNodes + "; "
                        + "Sem 'OneWay' = " + oneWayNaoIdentificados + "; Sem 'Tipo Highway' = " + highWayNaoIdentificados+"; Sem 'Way Name' = "+nomeWayNaoIdentificados);

        eliminarNodesNaoRua();

        if (calcWaysPorNode) {
            calcularQuantosWaysPorNode();
        }

        //encontrarNodesGemeos(metrosMaxParaSerNodeGemeo);

        criarArestasDirecionadas(arquivoSUMO);

        cadastrarVizinhos();

    }

    public boolean isPointNearAresta(int aresta, double lat, double lon){
         
        if(!between(lat,node_lat[fromNodI.get(aresta)] + arestaExtensaoGRAUS.get(aresta),node_lat[fromNodI.get(aresta)] - arestaExtensaoGRAUS.get(aresta), arestaExtensaoGRAUS.get(aresta)/8))
            return false; //só em Lat, distância supera extensão da aresta (GRAUS), relação a from
        if(!between(lon,node_lon[fromNodI.get(aresta)] + arestaExtensaoGRAUS.get(aresta),node_lon[fromNodI.get(aresta)] - arestaExtensaoGRAUS.get(aresta), arestaExtensaoGRAUS.get(aresta)/8))
            return false; //só em Lon, distância supera extensão da aresta (GRAUS), relação a from
        if(distanciaPontosMETROS(lat, lon, node_lat[toNodI.get(aresta)], node_lon[toNodI.get(aresta)]) > arestaExtensaoMETROS.get(aresta))
            return false; //não está no RAIO de distância do tamanho da aresta do nó TO (METROS)
    
        //se distância for menor que tamanho da aresta do nó FROM, então está na região de interesse (METROS)
        return (distanciaPontosMETROS(lat, lon, node_lat[fromNodI.get(aresta)], node_lon[fromNodI.get(aresta)]) < arestaExtensaoMETROS.get(aresta)); 
    }
    
    
    public String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));

    }

    public void eliminarNodesNaoRuaOLD() {
        /*String[] node_id;
    private Double[] node_lat;
    private Double[] node_lon;
    private int contNodes = 0;*/

        System.out.println("PROC: Iniciando eliminação de nodes não rodoviários... " + horaAtual());

        String[] node_idX = new String[(int) (contNodes * fatorRedux)];
        Double[] node_latX = new Double[(int) (contNodes * fatorRedux)];
        Double[] node_lonX = new Double[(int) (contNodes * fatorRedux)];
        int contAt = 0;

        //calcula quantos ways por node
        for (int a = 0; a < contNodes; a++) {
            for (int b = 0; b < wayCount; b++) {
              //  for (int c = 0; c < way_nodes[b].size(); c++) {
                    if (way_nodes[b].contains(node_id[a])) {
                        //encontrou 1 caso em que o node é útil. Repassa para novo vetor
                        node_idX[contAt] = node_id[a];
                        node_latX[contAt] = node_lat[a];
                        node_lonX[contAt] = node_lon[a];
                        contAt++;
                        //c = way_nodes[b].size();
                        b = wayCount;
                    }
               // }
            }
        }

        //substitui vetores completos pelos reduzidos
        node_id = node_idX;
        node_lat = node_latX;
        node_lon = node_lonX;

        System.out.println("OK: Eliminou "+(contNodes-contAt)+" nodes não rodoviários! (Utilizando " + contAt + "/" + (int) (contNodes * fatorRedux) + ") " + horaAtual());
        contNodes = contAt;

    }

    
        public void eliminarNodesNaoRua() {
        /*String[] node_id;
    private Double[] node_lat;
    private Double[] node_lon;
    private int contNodes = 0;*/

        System.out.println("PROC: Iniciando eliminação de nodes não rodoviários (REDUX)... " + horaAtual());

        boolean [] manter = new boolean[contNodes];
        for (int a = 0; a < contNodes; a++) 
            manter[a] = false;

        
        int contAt = 0;
        int ind;

        //passando por todos os ways, encontrando nós.
            for (int b = 0; b < wayCount; b++) {
                for (int c = 0; c < way_nodes[b].size(); c++) {
                    ind = getNodeIndex(way_nodes[b].get(c));
                    if(ind>=0)
                        manter[ind]=true;
                }
            }
        
       
        //conta quantos serão mantidos
       for (int a = 0; a < contNodes; a++)
           if(manter[a])
               contAt++;
        //cria vetores na medida certa
        String[] node_idX = new String[contAt];
        Double[] node_latX = new Double[contAt];
        Double[] node_lonX = new Double[contAt];
        contAt=0;
        
        for (int a = 0; a < contNodes; a++){
            if(manter[a]){
                node_idX[contAt] = node_id[a];
                node_latX[contAt] = node_lat[a];
                node_lonX[contAt] = node_lon[a];    
                contAt++;
            }
        }

        //substitui vetores completos pelos reduzidos
        node_id = node_idX;
        node_lat = node_latX;
        node_lon = node_lonX;

        System.out.println("OK: Eliminou "+(contNodes-contAt)+" nodes não rodoviários! (Utilizando " + contAt + "/" + (int) (contNodes) + ") " + horaAtual());
        contNodes = contAt;
    }
    
    
    public void calcularQuantosWaysPorNode() {
        //Array way_nodes[wayEmProcesso],  tamanho wayCount
        //String[] node_id, tamanho contNodes
        int[] cont = new int[contNodes];

        for (int a = 0; a < contNodes; a++) {
            cont[a] = 0;
        }

        //calcula quantos ways por node
        for (int a = 0; a < contNodes; a++) {
            for (int b = 0; b < wayCount; b++) {
                for (int c = 0; c < way_nodes[b].size(); c++) {
                    if (way_nodes[b].get(c).equals(node_id[a])) {
                        cont[a]++;
                    }
                }
            }
        }

        DecimalFormat df2 = new DecimalFormat(".##");
        DescriptiveStatistics stats = new DescriptiveStatistics();

        //summary de nodes
        for (int a = 0; a < contNodes; a++) {
            stats.addValue(cont[a]);
        }
        System.out.println("STATS: WAYS por NODE(un). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

        waysPorNode = cont;

        /*   int med = cont[0], min, max, fmin = 0, fmax = 0;
        //faz média, min e max
        min = cont[0];
        max = cont[0];

        for (int a = 1; a < contNodes; a++) {
            med = med + cont[a];
            if (cont[a] > max) {
                max = cont[a];
                fmax = 1;
            } else if (cont[a] < min) {
                min = cont[a];
                fmin = 1;
            } else if (cont[a] == min) {
                fmin++;
            } else if (cont[a] == max) {
                fmax++;
            }
        }

        
        
        System.out.println("CONT WAYS por NODE: Min: " + (min) + " (x" + fmin + "); Med: " + (med / contNodes) + "; Max: " + max + " (x" + fmax + ");");*/
    }

    /**
     * @return the node_id
     */
    public String[] getNode_id() {
        return node_id;
    }

    public int getWaysDoNode(int x) {
        if (x < contNodes) {
            return waysPorNode[x];
        } else {
            return -1;
        }
    }

    public boolean nodeComApenasUmWay(int x) {
        if (x < contNodes) {
            return waysPorNode[x] == 1;
        } else {
            return false;
        }
    }

    /**
     * @return the node_lat
     */
    public Double[] getNode_lat() {
        return node_lat;
    }

    /**
     * @return the node_lon
     */
    public Double[] getNode_lon() {
        return node_lon;
    }

    /**
     * @return the contNodes
     */
    public int getContNodes() {
        return contNodes;
    }

    /**
     * @return the way_id
     */
    public String[] getWay_id() {
        return way_id;
    }

    /**
     * @return the way_nodes
     */
    public ArrayList<String>[] getWay_nodes() {
        return way_nodes;
    }

    /**
     * @return the oneWay
     */
    public boolean[] getOneWay() {
        return oneWay;
    }

    /**
     * @return the wayCount
     */
    public int getWayCount() {
        return wayCount;
    }

    public void criarROIs() {

        //encontrar nodes opostos (contNodes, node_lat)
        menorLat = node_lat[0];
        maiorLat = node_lat[0];
        menorLon = node_lon[0];
        maiorLon = node_lon[0];

        for (int i = 1; i < contNodes; i++) {
            if (node_lat[i] < getMenorLat()) {
                menorLat = node_lat[i];
            } else if (node_lat[i] > getMaiorLat()) {
                maiorLat = node_lat[i];
            }
            if (node_lon[i] < getMenorLon()) {
                menorLon = node_lon[i];
            } else if (node_lon[i] > getMaiorLon()) {
                maiorLon = node_lon[i];
            }
        }

        //ROInumberLat: distancia lat / tamanho ROI
        ROInumberLat = ((int) ((distM(getMaiorLat(), getMenorLat()) / getROIsizeM())));
        //ROInumberLon: distancia lon / tamanho ROI
        ROInumberLon = ((int) ((distM(getMaiorLon(), getMenorLon()) / getROIsizeM())));

        System.out.println("RSLT: Map Lat: [" + getMenorLat() + "," + getMaiorLat() + "] Dist " + distM(getMaiorLat(), getMenorLat()) + "m, " + getROInumberLat() + " ROIs Lat; Lon: [" + getMenorLon() + "," + getMaiorLon() + "] Dist " + distM(getMaiorLon(), getMenorLon()) + "m, " + getROInumberLon() + " ROIs Lon;" + horaAtual());

    }

    public double distM(double a, double b) {

        if (a > b) {
            return (a - b) / 0.0000089;
        } else {
            return (b - a) / 0.0000089;
        }
    }

    /**
     * @return the ROIsizeM
     */
    public int getROIsizeM() {
        return ROIsizeM;
    }

    /**
     * @return the ROInumberLat
     */
    public int getROInumberLat() {
        return ROInumberLat;
    }

    /**
     * @return the ROInumberLon
     */
    public int getROInumberLon() {
        return ROInumberLon;
    }

    /**
     * @return the menorLat
     */
    public Double getMenorLat() {
        return menorLat;
    }

    /**
     * @return the maiorLat
     */
    public Double getMaiorLat() {
        return maiorLat;
    }

    /**
     * @return the menorLon
     */
    public Double getMenorLon() {
        return menorLon;
    }

    /**
     * @return the maiorLon
     */
    public Double getMaiorLon() {
        return maiorLon;
    }

    public String[] getWayKind() {
        return this.way_kind;
    }

    private double mod(double x, double y) {
        if (x > y) {
            return (x - y);
        } else {
            return (y - x);
        }
    }

    /**
     * @return the fromNod
     */
    public ArrayList<String> getFromNod() {
        if (temArestas) {
            return fromNod;
        } else {
            criarArestasDirecionadas("");
            return fromNod;
        }
    }

    /**
     * @return the toNod
     */
    public ArrayList<String> getToNod() {
        if (!temArestas) {
            criarArestasDirecionadas("");
        }

        return toNod;
    }

    /**
     * @return the fromNodI
     */
    public ArrayList<Integer> getFromNodI() {
        if (!temArestas) {
            criarArestasDirecionadas("");
        }

        return fromNodI;
    }

    public int getFromNodI(int index) {
        if (!temArestas) {
            criarArestasDirecionadas("");
        }

        return fromNodI.get(index);
    }

    public int getToNodI(int index) {
        if (!temArestas) {
            criarArestasDirecionadas("");
        }

        return toNodI.get(index);
    }

    /**
     * @return the toNodI
     */
    public ArrayList<Integer> getToNodI() {
        if (!temArestas) {
            criarArestasDirecionadas("");
        }

        return toNodI;
    }

    
       public boolean salvarDat(String name) {

        try {
            try (FileOutputStream arquivoGrav = new FileOutputStream(name + ".dat")) {
                try (ObjectOutputStream objGravar = new ObjectOutputStream(arquivoGrav)) {
                    objGravar.writeObject(this);
                    objGravar.flush();
                    objGravar.close();
                }
                arquivoGrav.flush();
                arquivoGrav.close();
            }
            System.out.println("OK: Mapping '" + name + "' salvo em .dat!");
            return true;
        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println("ERROR: Falha ao salvar .dat de " + name);
            return false;
        }

    }
    
    
    
}


