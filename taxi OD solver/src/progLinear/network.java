/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.

        network net = new network();
        codeGenerator cod = new codeGenerator();
        cod.printCodigo(net);

 */
package progLinear;

import java.util.ArrayList;
import java.util.Random;
import taxi.od.solver.Mapping;
import taxi.od.solver.ODMatrix;
import taxi.od.solver.VirtualSensors;

/**
 * @author Luciano
 */
public class network {

    boolean listarCaminhos = false;

    int[] nos;

    private int[] parOD1;
    private int[] parOD2;
    private double[] quantidadeCarros;

    private int[] aresta1;
    private int[] aresta2;
    //private int[] codAresta;
    int contA = 0;

    private int[][] trajeto;
    private double[][] probTrajeto;
    int[] tamanhoTrajeto;

    int[] caminhoAux;
    int caminhoAuxTam = 0;
    int distanciaM;
    boolean encontrou;
    Random gerador;

    int numClusters;
    private int tempoPriori;
    private int tempoProblema;
    private int batch;

    private ArrayList<Integer>[][] ODparIndArestaMaisMov2;
    private ArrayList<Integer>[][] ODparIndArestaMaisMovENCONTRADOS2;
    private ArrayList[][] ODparIndArestaMaisMovENCONTRADOSindiceAresta;
    private ArrayList[][] ODparIndArestaMaisMovENCONTRADOS2indiceAresta;
    private ArrayList<Integer>[][] ODparIndArestaMaisMov;
    private ArrayList<Integer>[][] ODparIndArestaMaisMovENCONTRADOS;
    private boolean[] parODcoberto;
    private int[] sensorAresta;

    VirtualSensors virtualSensors;

    public network() {
        //48 nós. ?? aresgeradortas. 14 pontos O-D (1 até 14)
        gerador = new Random();

        declaracaoArestas();
        gerarParesOD();
        gerarCaminhos();
        gerarQuantCarros();

    }

    public int getNumeroSensores(){
    return sensorAresta.length;
    }
    
    public network(int numSens, int tempoPriori1, int tempoProblema1, int batch1, VirtualSensors vt, ODMatrix odm, Mapping map, boolean sensoresFluxoNotRotas, boolean sensoresMaisRotas) {
        //48 nós. ?? aresgeradortas. 14 pontos O-D (1 até 14)
        gerador = new Random();
        virtualSensors = vt;
        numClusters = odm.getNumeroClusters();

        tempoPriori = tempoPriori1;
        tempoProblema = tempoProblema1;
        batch = batch1;
        int [] res;
        
        
        if (sensoresFluxoNotRotas) {
            if(sensoresMaisRotas) //apenas por maiores fluxos
                res = virtualSensors.getArestasMaismovimentadasIndex(numSens, tempoPriori);
            else   // por fluxos/rotas
                res = virtualSensors.getArestasMaismovimentadasMenosRotasIndex(numSens, tempoPriori, odm);
        } else {
            if(sensoresMaisRotas)
                res = virtualSensors.getArestasMaisRotasIndex(odm, numSens, tempoPriori);
            else
                res = virtualSensors.getArestasMenosRotasIndex(odm, numSens, tempoPriori);
        }
        
        sensorAresta = vt.getArestasMaismovimentadasIndexARESTA();

        cadastrarSomenteArestasSensores(sensorAresta,
                res, vt.getArestasMaismovimentadasIndexTONODE());

        cadastrarProbabilidadesArestas(vt, odm, sensorAresta);

        popularParesOD();

        gerarQuantCarros(odm, tempoProblema, batch);   //fluxos por pares OD

    }

    public void popularParesOD() {

        parOD1 = new int[numClusters * numClusters];
        parOD2 = new int[numClusters * numClusters];
        int tr;

        for (int o = 0; o < numClusters; o++) {
            for (int d = 0; d < numClusters; d++) {
                tr = o * numClusters + d;
                parOD1[tr] = o;
                parOD2[tr] = d;
            }
        }

    }

    public String codPrintTodosODPar(String c) {
        String z = "\nprintf \"" + c + " \";\n";
        
        for (int o = 0; o < numClusters; o++) {

            for (int p = 0; p < (numClusters); p = p + 10) { //começo for


                z = z + "\nprintf \"";
                for (int d = p; d < (p+10); d++) {
                    if(d<numClusters)
                    z = z + "%3f ";
                }
                if (o < (numClusters - 1)) { //se não for a última linha
                    z = z + "\"";
                } else { //se for a última linha
                    if(p<(numClusters-10)) //se não for o último bloco
                        z = z + "\"";
                    else
                        z = z + "\\n\\n\""; //se for o último bloco
                }
                for (int d = p; d < (p+10); d++) {
                    if(d<numClusters)
                    z = z + ", OD" + o + "to" + d;
                }
                z = z + ";";

            }//fim for
        }

        return z;
    }

    public void gerarQuantCarros(ODMatrix odm, int tempo, int bat) {

        quantidadeCarros = new double[numClusters * numClusters];
        int tr;
        for (int o = 0; o < numClusters; o++) {
            for (int d = 0; d < numClusters; d++) {
                tr = o * numClusters + d;
                quantidadeCarros[tr] = odm.getODMatrixClustersBatch(o, d, tempo, bat);
            }
        }

        System.out.println("OK: Quantidade original de transito por par OD definida!");

    }

    public void cadastrarProbabilidadesArestas(VirtualSensors vt, ODMatrix ODmatrix, int[] sensores) {

        trajeto = new int[numClusters * numClusters][4200];
        probTrajeto = new double[numClusters * numClusters][4200];
        tamanhoTrajeto = new int[numClusters * numClusters];
        int tr;
        int cont = 0;
        double p;

       // descobrirODparIndArestaMaisMov(numClusters, ODmatrix, getTempoPriori());// ????

        for (int o = 0; o < numClusters; o++) {
            for (int d = 0; d < numClusters; d++) {
                tr = o *  numClusters + d;  //tr = o *  numClusters + d; ?????
                /*for (int ars = 0; ars < ODparIndArestaMaisMov[o][d].size(); ars++) {
                    if(ODparIndArestaMaisMov[o][d].get(ars) !=-1){     //se Prob por aresta está nos sensores ativos
                    trajeto[tr][tamanhoTrajeto[tr]] = sensores[ODparIndArestaMaisMov[o][d].get(ars)];
                    if(sensores[ODparIndArestaMaisMov[o][d].get(ars)] != ODmatrix.getODparArestaIndexGeralAresta(o, d, getTempoPriori(), ars))
                        System.out.println("ERROR: sensores[ODparIndArestaMaisMov["+o+"]["+d+"].get("+ars+")] (="+sensores[ODparIndArestaMaisMov[o][d].get(ars)]+") != ODmatrix.getODparArestaIndexGeralAresta("+o+","+ d+", "+getTempoPriori()+", "+ars+") (="+ODmatrix.getODparArestaIndexGeralAresta(o, d, getTempoPriori(), ars)+") (network.CadastrarProbabilidadesArestas)");
                    
                    probTrajeto[tr][tamanhoTrajeto[tr]] = ODmatrix.getODParArestaCont(o, d, getTempoPriori(), ars);
                    tamanhoTrajeto[tr]++;
                    cont++;
                    }
                }*/
                //versão solucao alternativa
                for (int ars = 0; ars < sensores.length; ars++) {
                    p = ODmatrix.getProbUsoArestaPorParODeIndexAresta(o, d, getTempoPriori(), sensores[ars]);
                    if (p > 0.00) {
                        trajeto[tr][tamanhoTrajeto[tr]] = sensores[ars];
                        probTrajeto[tr][tamanhoTrajeto[tr]] = p;
                        tamanhoTrajeto[tr]++;
                        cont++;
                    }

                }

            }
        }
        System.out.println("OK: Cadastrou " + cont + " probabilidades de uso de aresta por par OD!");

    }

    public void cadastrarSomenteArestasSensores(int[] sensores, int[] fromIndex, int[] toIndex) {
        aresta1 = new int[sensores.length];
        aresta2 = new int[sensores.length];

        for (int x = 0; x < sensores.length; x++) {
            aresta1[x] = fromIndex[x];
            aresta2[x] = toIndex[x];
        }
        System.out.println("OK: " + sensores.length + " arestas criadas.");
    }

    public int[] getCodigosArestas() {

        return sensorAresta;
    }

    public String STRINGsomaRotasPassamPorSensor2(int codAresta) {
        String s = "(";

        /*for (int a = 0; a < parOD1.length; a++) {
            for (int b = 0; b < tamanhoTrajeto[a] - 1; b++) {
                if (trajeto[a][b] == ax && trajeto[a][b + 1] == bx) {
                    s = s + "OD" + parOD1[a] + "to" + parOD2[a] + "+";
                }
            }
        }*/
        for (int tr = 0; tr < (numClusters * numClusters); tr++) {
            for (int pij = 0; pij < tamanhoTrajeto[tr]; pij++) {
                if (trajeto[tr][pij] == codAresta) {
                    s = s + "OD" + parOD1[tr] + "to" + parOD2[tr] + "*" + probTrajeto[tr][pij] + "  +";
                }
            }
        }

        if (s.length() < 2) {
            return "0";
        }

        return s.substring(0, s.length() - 2) + ")";
    }

    public String STRINGcoberturaDeRotaPorSensor(int ax, int bx) {
        String s = "\nsubj to rCob" + ax + "_" + bx + ": (";

        for (int a = 0; a < parOD1.length; a++) {
            if (parOD1[a] == ax && parOD2[a] == bx) {
                for (int b = 0; b < tamanhoTrajeto[a] - 1; b++) {
                    s = s + "b" + trajeto[a][b] + "sen" + trajeto[a][b + 1] + "+";
                }
            }
        }

        return s.substring(0, s.length() - 1) + ")>=2;";
    }

    public int getFluxoPorAresta(int numeroAresta) {

        return virtualSensors.getContArestaBatch(numeroAresta, getTempoProblema(), getBatch());

    }

    /*public int INTSomaRotasPassamPorSensor(int ax, int bx) {
        int sm = 0;

        for (int a = 0; a < parOD1.length; a++) {
            for (int b = 0; b < tamanhoTrajeto[a] - 1; b++) {
                if (trajeto[a][b] == ax && trajeto[a][b + 1] == bx) {
                    sm += quantidadeCarros[a];
                }
            }
        }

        return sm;
    }*/
    public void gerarQuantCarros() {

        quantidadeCarros = new double[parOD1.length];

        for (int a = 0; a < parOD1.length; a++) {
            quantidadeCarros[a] = 8 + gerador.nextInt(20);
            // System.out.println("OD"+parOD1[a]+"to"+parOD2[a]+": "+quantidadeCarros[a]);
        }

    }

    public double getQuantidadeCarros(int par) {

        return quantidadeCarros[par];

    }

    public void printCaminho(int a) {
        String x = getParOD1()[a] + "->" + getParOD2()[a] + ": ";
        for (int c = 0; c < tamanhoTrajeto[a]; c++) {
            //   System.out.println("a="+a+" c="+c);
            x = x + " " + getTrajeto()[a][c];

        }

        x = x + "; (" + tamanhoTrajeto[a] + ")";

        System.out.println(x);
    }

    public void gerarCaminhos() {
        setTrajeto(new int[getParOD1().length][100]);
        tamanhoTrajeto = new int[getParOD1().length];

        for (int a = 0; a < getParOD1().length; a++) { // de parOD em parOD
            preparaBusca();

            while (distanciaM > 20) {
                searchWay(getParOD2()[a], getParOD1()[a], new int[200], 0);
            }

            getTrajeto()[a] = caminhoAux;
            tamanhoTrajeto[a] = distanciaM + 2;
            if (listarCaminhos) {
                printCaminho(a);
            }
        }

        System.out.println("OK: Gerou todos os caminhos entre pontos OD!");
    }

    public void preparaBusca() {
        nos = new int[49];
        for (int a = 1; a < 49; a++) {
            nos[a] = 150;
        }
        caminhoAux = new int[200];
        caminhoAuxTam = 0;
        distanciaM = 150;
        encontrou = false; // cuidado. Pode pegar o primeiro caminho que encontrar
    }

    //quer encontrar nó FIND. recebe lista de nós para procurar.
    public void searchWay(int find, int atual, int[] path, int distancia) {

        System.out.println("searchWay(" + find + ", " + atual + ", .., " + distancia + ")");

        if (distancia >= distanciaM) /*||distancia>nos[atual] )*/ {
            return;
        }

        if (encontrou) {
            return;
        }

        //  System.out.println("CALL p "+find+" de "+atual+ " dist "+distancia);
        int[] path2 = new int[200];// = path;

        for (int z = 0; z < distancia; z++) {
            path2[z] = path[z];
        }

        nos[atual] = distancia;
        path2[distancia] = atual;

        //vê se está na vizinhança;
        for (int a = 0; a < contA; a++) {
            if ((getAresta1()[a] == find && getAresta2()[a] == atual)
                    || (getAresta2()[a] == find && getAresta1()[a] == atual)) {
                //se estiver, retorna caminho até aqui
                distanciaM = distancia;
                caminhoAuxTam = distancia + 2;
                path2[distancia + 1] = find;
                caminhoAux = path2;
                // encontrou = true;
                return;
            }
        }

        int v;
        //se não estiver, indica próximos nós a buscar
        for (int ab = 0; ab < contA * 2; ab++) {
            v = (gerador.nextInt(contA));
            if (getAresta2()[v] == atual && nos[getAresta1()[v]] > distancia) {
                searchWay(find, getAresta1()[v], path2, distancia + 1);
            } else if (getAresta1()[v] == atual && nos[getAresta2()[v]] > distancia) {
                searchWay(find, getAresta2()[v], path2, distancia + 1);
            }

        }

    }

    public void na(int a, int b) {
        novaAresta(a, b);
    }

    public void novaAresta(int a, int b) {
        aresta1[contA] = a;
        aresta2[contA] = b;
        contA++;
        //aresta1[contA]=b;
        //aresta2[contA]=a;
        //contA++;
    }

    public void gerarParesOD() {

        //os de fora visitam os de dentro
        // 8 dentro, 6 fora. 6x8 = 48. Ida e volta: 96
        parOD1 = new int[96];
        parOD2 = new int[96];
        int x = 0;

        for (int a = 1; a < 9; a++) {
            for (int b = 9; b < 15; b++) {

                parOD1[x] = a;
                parOD2[x] = b;
                x++;
                parOD1[x] = b;
                parOD2[x] = a;
                x++;
            }
        }
        System.out.println("OK: " + x + " pares O-D gerados.");

    }

    public void cadastrarTodasArestas(int[] fromIndex, int[] toIndex) {
        aresta1 = new int[fromIndex.length];
        aresta2 = new int[fromIndex.length];

        for (int x = 0; x < fromIndex.length; x++) {
            aresta1[x] = fromIndex[x];
            aresta2[x] = toIndex[x];
        }
        System.out.println("OK: " + fromIndex.length + " arestas criadas.");
    }

    public void declaracaoArestas() {

        aresta1 = new int[76];
        aresta2 = new int[76];

        //vertical h0 h1
        na(15, 9);
        na(16, 9);
        na(18, 9);
        na(20, 9);
        //horizontal 1
        na(15, 16);
        na(16, 17);
        na(17, 18);
        na(18, 19);
        na(19, 20);
        //vertical h1 h3
        na(10, 15);
        na(15, 22);
        na(1, 23);
        na(16, 21);
        na(21, 24);
        na(17, 2);
        na(2, 25);
        na(18, 26);
        na(19, 3);
        na(3, 27);
        na(20, 28);
        na(20, 14);
        //horizontal 2 e 3
        na(21, 2);
        na(10, 22);
        na(22, 23);
        na(23, 24);
        na(24, 25);
        na(25, 26);
        na(27, 28);
        na(28, 14);
        //vertical h3 h5
        na(22, 32);
        na(23, 4);
        na(4, 34);
        na(24, 29);
        na(29, 35);
        na(25, 5);
        na(5, 36);
        na(26, 30);
        na(30, 37);
        na(6, 38);
        na(28, 31);
        na(14, 31);
        na(31, 39);
        //horizontal 4 e 5
        na(4, 29);
        na(29, 5);
        na(30, 6);
        na(32, 33);
        na(33, 34);
        na(34, 35);
        na(35, 36);
        na(36, 37);
        na(37, 38);
        na(38, 39);
        //vertical h5 h7
        na(32, 11);
        na(33, 11);
        na(40, 11);
        na(35, 40);
        na(40, 43);
        na(43, 45);
        na(36, 7);
        na(7, 46);
        na(37, 41);
        na(41, 44);
        na(44, 47);
        na(38, 8);
        na(39, 42);
        na(42, 48);
        //horizontal 6 7    
        na(40, 7);
        na(41, 8);
        na(11, 45);
        na(45, 46);
        na(46, 47);
        na(47, 48);
        na(48, 13);
        //vertical end
        na(45, 12);
        na(47, 12);
        na(48, 12);

        System.out.println("OK: " + contA + " arestas criadas.");
    }

    /**
     * @return the aresta1
     */
    public int[] getAresta1() {
        return aresta1;
    }

    /**
     * @return the aresta2
     */
    public int[] getAresta2() {
        return aresta2;
    }

    /**
     * @return the trajeto
     */
    public int[][] getTrajeto() {
        return trajeto;
    }

    /**
     * @param trajeto the trajeto to set
     */
    public void setTrajeto(int[][] trajeto) {
        this.trajeto = trajeto;
    }

    /**
     * @return the parOD1
     */
    public int[] getParOD1() {
        return parOD1;
    }

    /**
     * @return the parOD2
     */
    public int[] getParOD2() {
        return parOD2;
    }

    public void descobrirODparIndArestaMaisMov(int clusters, ODMatrix ODmatrix, int tempoPriori) {
        ODparIndArestaMaisMov = new ArrayList[clusters][clusters];
        ODparIndArestaMaisMovENCONTRADOS = new ArrayList[clusters][clusters];

        ODparIndArestaMaisMov2 = new ArrayList[clusters][clusters];
        ODparIndArestaMaisMovENCONTRADOS2 = new ArrayList[clusters][clusters];

        int tempoPriori2 = tempoPriori;
        int cont = 0;
        int cob = 0;
        int reserva = -1;

        parODcoberto = new boolean[clusters * clusters];
        for (int c = 0; c < clusters * clusters; c++) {
            parODcoberto[c] = false;
        }

        for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) {

                ODparIndArestaMaisMov[o][d] = ODmatrix.encontrarIndicesArestasVetor(o, d, tempoPriori, sensorAresta); //tempoPriori1
                ODparIndArestaMaisMov2[o][d] = ODmatrix.encontrarIndicesArestasVetor(o, d, tempoPriori2, sensorAresta);

                ODparIndArestaMaisMovENCONTRADOS[o][d] = new ArrayList<>();
                ODparIndArestaMaisMovENCONTRADOS2[o][d] = new ArrayList<>();

                if (ODparIndArestaMaisMov[o][d].isEmpty()) {
                    // System.out.println("ALERT: Par OD " + o + "," + d + " sem arestas!");
                    parODcoberto[o * clusters + d] = false;
                } else {
                    for (int x = 0; x < ODparIndArestaMaisMov[o][d].size(); x++) {
                        if (ODparIndArestaMaisMov[o][d].get(x) != -1) {
                            ODparIndArestaMaisMovENCONTRADOS[o][d].add(x);

                            cont++;
                        }

                    }
                    parODcoberto[o * clusters + d] = true;
                    cob++;
                }
                //pt 2

                if (ODparIndArestaMaisMov2[o][d].isEmpty()) {
                    // System.out.println("ALERT: Par OD " + o + "," + d + " sem arestas!");
                    //parODcoberto[o * clusters + d] = false;
                } else {
                    for (int x = 0; x < ODparIndArestaMaisMov2[o][d].size(); x++) {
                        if (ODparIndArestaMaisMov2[o][d].get(x) != -1) {
                            ODparIndArestaMaisMovENCONTRADOS2[o][d].add(x);
                            //  cont++;
                        }

                    }
                    //parODcoberto[o * clusters + d] = true;
                    //cob++;
                }
            }
        }

        System.out.println("INFO: descobrirODparIndArestaMaisMov = " + cont + " casos! - " + cob + "/" + (clusters * clusters) + " pares O-D cobertos");
    }

    /**
     * @return the tempoPriori
     */
    public int getTempoPriori() {
        return tempoPriori;
    }

    /**
     * @return the tempoProblema
     */
    public int getTempoProblema() {
        return tempoProblema;
    }

    /**
     * @return the batch
     */
    public int getBatch() {
        return batch;
    }

}
