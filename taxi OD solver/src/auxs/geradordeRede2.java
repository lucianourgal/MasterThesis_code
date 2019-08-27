/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import taxi.od.solver.Mapping;
import taxi.od.solver.VirtualSensors;
import taxi.od.solver.taxi_Trip_Instance;

/**
 *
 * @author lucia
 */
public class geradordeRede2 {

    private ArrayList<Double> matrizPriori;
    private double somaMatrixPriori = 0;

    private ArrayList<Integer> tamanhoCaminhos;
    private int numPontosOD = 30;

    private ArrayList<Double> latNo;
    private ArrayList<Double> lonNo;
    private ArrayList<String> codNo;

    private ArrayList<Integer> fromNo;
    private ArrayList<Integer> toNo;
    private ArrayList<Integer> tipoLink;
    private ArrayList<String> fromNoC;
    private ArrayList<String> toNoC;
    private ArrayList<Integer> capacidade;
    private ArrayList<Integer> capacidadeSemaforo;
    private ArrayList<Integer> contLink;
    private ArrayList<Integer> velocidade;
    private ArrayList<Double> volDelayFunction1Hum;
    private ArrayList<Double> volDelayFunction2Dois;
    private ArrayList<Double> tempoLinkLivreEmSeg;
    private double[] fluxosParaBPR;

    private double[] geh;

    private ArrayList<Integer> producao;
    private ArrayList<Integer> atracao;
    private ArrayList<Double> latBorda;
    private ArrayList<Double> lonBorda;
    private ArrayList<Integer> noProxBorda;

    private int linksComContadores = 0;
    private Mapping mape;

    private ArrayList<Integer>[][][] paths;
    private int[][] contPaths;
    private ArrayList<Double>[][] probPaths;

    ArrayList<Integer> doParOD = new ArrayList<>();
    ArrayList<Double> probODaresta = new ArrayList<>();
    ArrayList<Integer> daAresta = new ArrayList<>();

    /**
     * Recebe MOD inicial
     *
     */
    private void gerarAssignmentMatrix_BPR(Mapping map) {
        System.out.println("PROC: Gerando assignment matrix por BPR... " + horaAtual());
        setMape(map);
        double tempoMed = 99999.2;
        double last = 99999.3;
        int it = 0;
        iniciarPathsEFlows();

        ArrayList<Integer> doParODaux = new ArrayList<>();
        ArrayList<Double> probODarestaAux = new ArrayList<>();
        ArrayList<Integer> daArestaAux = new ArrayList<>();

        while (tempoMed < (last)) {
//iterar em busca de reduzir custos BPR dos links (mudar probabilidade por path)
//quando não for mais possível melhorar o (tempo médio da rede), sai do laço

            doParODaux = new ArrayList<>(); //uma iteracao atrasados, para nao pegar a ultima iteracao, que tem mais custo
            probODarestaAux = new ArrayList<>();
            daArestaAux = new ArrayList<>();
            for (int x = 0; x < doParOD.size(); x++) {
                doParODaux.add(doParOD.get(x));
                probODarestaAux.add(probODaresta.get(x));
                daArestaAux.add(daAresta.get(x));
            }

//gera paths por par OD
            criarNovoPathParaTodosParesOD(0.33);
//criarPathsSimilaresParaTodosParesOD(1);
            normalizePathChoice();//distribuição de prob por path igual para todos

//cria "assignment matrix" = lista de probabilidade de uso VS numero da aresta VS numero do par OD
            criarAssignmentMatrix();
//salva atual

//incia laço de melhora de (Tempo médio da rede)
//calcular quantidade de veículos por link
            calcularFluxosPorODMPrioriEAssignment();

            last = tempoMed;
            tempoMed = calcTempoMedEntreParesOD();
            System.out.println(" tempoMed=" + tempoMed);
            it++;
        }

        doParOD = new ArrayList<>();//uma iteracao atrasados, para nao pegar a ultima iteracao, que tem mais custoF
        probODaresta = new ArrayList<>();
        daAresta = new ArrayList<>();
        for (int x = 0; x < doParODaux.size(); x++) {
            doParOD.add(doParODaux.get(x));
            probODaresta.add(probODarestaAux.get(x));
            daAresta.add(daArestaAux.get(x));
        }

        System.out.println("OK: Convergiu em " + it + " iterações. " + doParOD.size() + " prob aresta/parOD. "
                + "TempoMed=" + tempoMed + "; somaMatrixPriori = " + somaMatrixPriori);
    }

    public ArrayList<String> lerTxt(String file) {

        ArrayList<String> resp = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(file));
            scanner.useDelimiter("\n");
            while (scanner.hasNext()) {
                resp.add(scanner.next());
            }

        } catch (FileNotFoundException ex) {
            // Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resp;
    }

    public void estatisticasEngarrafamentoVias(double[] veiculosAresta, int[] indexArestas) {

        //capacidade, capacidadeSemaforo, contLink
        double[] velocidades = new double[veiculosAresta.length];
        double[] fatores = new double[veiculosAresta.length];

        for (int x = 0; x < veiculosAresta.length; x++) {
            //velocidade        
            velocidades[x] = this.BPR_calcAvgTravelTimeInLinkA(fromNo.get(indexArestas[x]), toNo.get(indexArestas[x]));
            //fator em relação á capacidade
            fatores[x] = veiculosAresta[x] / capacidade.get(indexArestas[x]);
            //transito total
        }

        //velocidade        
        this.printStatsD(velocidades, "Velocidades");
        //fator em relação á capacidade
        this.printStatsD(fatores, "Trafg.capac");
        //transito total
        this.printStatsD(veiculosAresta, "Fluxos");

    }

    public void criarPathsSimilaresParaTodosParesOD(double prob) {
        int c = 0;
        fatorFind = 1.10;

        for (int o = 0; o < this.numPontosOD; o++) {
            for (int d = 0; d < this.numPontosOD; d++) {
                probPaths[o][d] = new ArrayList<>();
                if (matrizPriori.get(o * numPontosOD + d) > 0) {

                    construirCaminhoMaisBarato(false, noProxBorda.get(o), noProxBorda.get(d)); //prob?

                    for (int a = 0; a < caminhosAlt.size(); a++) {
                        if (custosAlt.get(a) < custoFind * fatorFind) {

                            ArrayList<Integer> px = new ArrayList<>();
//px.add(noProxBorda.get(o));
                            for (int tp = 0; tp < caminhosAltTam.get(a); tp++) {
                                px.add(caminhosAlt.get(a)[tp]);
                            }
//px.add(noProxBorda.get(d));

                            probPaths[o][d].add(prob * custoFind / custosAlt.get(a));
                            paths[o][d][contPaths[o][d]] = px;
                            contPaths[o][d]++;

                        }
                    }

                    c += contPaths[o][d];
                }
            }
        }
        System.out.print("OK: " + c + " paths, para " + numPontosOD * numPontosOD + " pares OD!");
    }

    public static String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));
    }

    public void normalizePathChoice() {
        double sum;
        for (int o = 0; o < this.numPontosOD; o++) {
            for (int d = 0; d < this.numPontosOD; d++) {
                if (contPaths[o][d] > 1) {
                    sum = 0;
                    for (int p = 0; p < probPaths[o][d].size(); p++) {
                        sum += probPaths[o][d].get(p);
                    }

                    if (sum != 1.0) {
                        for (int p = 0; p < probPaths[o][d].size(); p++) {
                            probPaths[o][d].set(p, probPaths[o][d].get(p) / sum);
                        }
                    }
                }
            }
        }
    }

    public double calcTempoMedEntreParesOD() {
        double t = 0;
        int casos = 0;

        for (int o = 0; o < this.numPontosOD; o++) {
            for (int d = 0; d < this.numPontosOD; d++) {
                if (contPaths[o][d] > 0) {
                    casos++;
                    t += calcTempoMedParOd(o, d);
                }
            }
        }
        return somaMatrixPriori * t / casos;
    }

    public double calcTempoMedParOd(int o, int d) {
        double t = 0;
        for (int p = 0; p < contPaths[o][d]; p++) {
            t += calcTempoPath(o, d, p) * probPaths[o][d].get(p);
        }
        return (t * (contPaths[o][d] / somaMatrixPriori));
    }

    private double calcTempoPath(int o, int d, int p) {
        double t = 0;
//int aresta;
        if (paths[o][d][p].size() > 2) {
            for (int pc = 0; pc < paths[o][d][p].size() - 1; pc++) {
//aresta = mape.getArestaIndex(paths[o][d][p].get(pc),paths[o][d][p].get(pc+1));
                t = this.BPR_calcAvgTravelTimeInLinkA(paths[o][d][p].get(pc), paths[o][d][p].get(pc + 1));
            }
        }
//System.out.println(t);
        return t;
    }

    private void calcularFluxosPorODMPrioriEAssignment() {
        fluxosParaBPR = new double[this.getContArestas()];

        for (int f = 0; f < getContArestas(); f++) {
            fluxosParaBPR[f] = 0;
        }

        for (int x = 0; x < doParOD.size(); x++) {
            fluxosParaBPR[daAresta.get(x)] += probODaresta.get(x) * getFluxoPrioriODM(doParOD.get(x));
        }

    }

    private double getFluxoPrioriODM(int par) {
        return matrizPriori.get(par);
    }

    public ArrayList<Integer> getDOParOD() {
        if (doParOD.isEmpty()) {
            System.out.println("ERROR: doParOD vazio!");
        }
        return doParOD;
    }

    public ArrayList<Integer> getDaAresta() {
        if (doParOD.isEmpty()) {
            System.out.println("ERROR: doAresta vazio!");
        }
        return daAresta;
    }

    public ArrayList<Double> getProbODAresta() {
        if (doParOD.isEmpty()) {
            System.out.println("ERROR: probODareta vazio!");
        }
        return probODaresta;
    }

    private void criarAssignmentMatrix() {

        doParOD = new ArrayList<>();
        probODaresta = new ArrayList<>();
        daAresta = new ArrayList<>();

        ArrayList<Integer> aresta;
        ArrayList<Double> probAresta;
        int indexAresta;

        for (int o = 0; o < this.numPontosOD; o++) {
            for (int d = 0; d < this.numPontosOD; d++) {

                aresta = new ArrayList<>();
                probAresta = new ArrayList<>();

                for (int p = 0; p < contPaths[o][d]; p++) {
                    for (int pc = 0; pc < paths[o][d][p].size() - 1; pc++) {

                        indexAresta = getMape().getArestaIndex(paths[o][d][p].get(pc), paths[o][d][p].get(pc + 1));

                        if (aresta.contains(indexAresta)) { //se esse pedaço de caminho já aparecue antes para esse par OD
                            probAresta.set(aresta.indexOf(indexAresta), //acrescenta a probabilidade
                                    probAresta.get(aresta.indexOf(indexAresta)) + probPaths[o][d].get(p));
                        } else { //pedaço de caminho novo
                            aresta.add(indexAresta);
                            probAresta.add(probPaths[o][d].get(p));
                        }
                    }

                }

                for (int am = 0; am < aresta.size(); am++) {
                    doParOD.add(o * this.numPontosOD + d);
                    probODaresta.add(probAresta.get(am));
                    daAresta.add(aresta.get(am));
                }

            }//fim deste par OD
        }
//System.out.println("OK: Criou assignment matrix!");
    }

    public void iniciarPathsEFlows() {

        paths = new ArrayList[numPontosOD][numPontosOD][435];
        contPaths = new int[numPontosOD][numPontosOD];
        probPaths = new ArrayList[numPontosOD][numPontosOD];

        fluxosParaBPR = new double[this.getContArestas()];

    }

    public void criarNovoPathParaTodosParesOD(double prob) {
        int c = 0;
        for (int o = 0; o < this.numPontosOD; o++) {
            for (int d = 0; d < this.numPontosOD; d++) {
                if (matrizPriori.get(o * numPontosOD + d) > 0) {
                    criarNovoPath(o, d, prob);
                    c += contPaths[o][d];
                }
            }
        }
        System.out.print("OK: " + c + " paths, para " + numPontosOD * numPontosOD + " pares OD!");
    }

    public void criarNovoPath(int o, int d, double prob) {
        addNovoPath(o, d, construirCaminhoMaisBarato(false, noProxBorda.get(o), noProxBorda.get(d)), prob);
    }

    public void addNovoPath(int o, int d, ArrayList<Integer> nos, double prob) {

        paths[o][d][contPaths[o][d]] = nos;

        if (contPaths[o][d] == 0) {
            probPaths[o][d] = new ArrayList<>();
        }

        probPaths[o][d].add(prob);

        if (nos.size() > 0) {
            contPaths[o][d]++;
        }

    }

    boolean usarGemeos;

//gerador de paths
    public ArrayList<Integer> construirCaminhoMaisBarato(boolean usarGemeos1, int inicio, int fim) {

        usarGemeos = usarGemeos1;
        int repeat = 3;
        ArrayList<Integer> posNodesEmMapa = new ArrayList<>();
        posNodesEmMapa.add(inicio);
        posNodesEmMapa.add(fim);
        ArrayList<Integer> novasPos;
        boolean con;

//de node cadastrado em node cadastrado
        for (int n = 0; n < posNodesEmMapa.size() - 1; n++) {

            con = getMape().existeVizinho(posNodesEmMapa.get(n), posNodesEmMapa.get(n + 1), usarGemeos);

//caso não seja uma conexão direta
            if (!con) {

//encontrar conexão
                novasPos = new ArrayList<>();

                for (int n2 = 0; n2 <= n; n2++) //adiciona anteriores aos nós analisados
                {
                    novasPos.add(posNodesEmMapa.get(n2));
                }

//busca caminhos >> PARTE PRINCIPAL <<
                preparaBusca(getMape().getContNodes(), getMape().getContArestas());
                searchWay(posNodesEmMapa.get(n + 1), posNodesEmMapa.get(n), new int[maxDist + 1], (short) 0, 0.0);

                for (int t = 0; t < caminhoAuxTam; t++) {
                    novasPos.add(caminhoAux[t]);
                }

                for (int n2 = (n + 1); n2 < posNodesEmMapa.size(); n2++) { //adiciona posteriores
                    novasPos.add(posNodesEmMapa.get(n2));
                }

//readicionar nós á cadeia principal
                posNodesEmMapa = new ArrayList<>();
                for (int x = 0; x < novasPos.size() - 1; x++) {
                    if (!novasPos.get(x).equals(novasPos.get(x + 1))) { //se não for igual ao próximo elemento, adiciona
                        posNodesEmMapa.add(novasPos.get(x));// nodes.add(novos.get(x));
                    }
                }
                posNodesEmMapa.add(novasPos.get(novasPos.size() - 1));

//eliminar nodes repetidos da trip
                ArrayList<Integer> aux2 = new ArrayList<>();
                if (posNodesEmMapa.size() > 0) {
                    aux2.add(posNodesEmMapa.get(0));//aux.add(getNodes().get(0));
                }
                boolean rep;
                for (int a = 1; a < posNodesEmMapa.size(); a++) {
                    rep = false;
                    for (int x = (a - 1); x >= 0 && (x >= (a - 4)); x--) {
                        if (posNodesEmMapa.get(a).equals(posNodesEmMapa.get(x))) {
                            rep = true;
                            x = -1; //sai do laço
                        }
                    }
                    if (!rep) {
                        aux2.add(posNodesEmMapa.get(a)); //aux.add(getNodes().get(a));
                    }
                }
                posNodesEmMapa = aux2;
                n = n + caminhoAuxTam;
            }

            if (n == (posNodesEmMapa.size() - 3) && repeat > 0) {
                n = 0;
                repeat--;
            }
        }

        nos = null;
        caminhoAux = null;

        if (posNodesEmMapa.size() < 3) {
            posNodesEmMapa = new ArrayList<>();
        }

        return posNodesEmMapa;
    }

    Double[] nos;
    boolean encontrou;
    int[] caminhoAux;
    int caminhoAuxTam = 0;
    int distanciaM;
    int contArestas = 0;
    int maxDist = 45;
    double custoFind;

    double fatorFind = 1.0;
    ArrayList<int[]> caminhosAlt;
    ArrayList<Integer> caminhosAltTam;
    ArrayList<Double> custosAlt;

    public void preparaBusca(int nodes, int arestas) {
        nos = new Double[nodes];
        for (int a = 0; a < nodes; a++) {
            nos[a] = 3000000.0;//(short)nodes;
        }

        custosAlt = new ArrayList<>();
        caminhosAlt = new ArrayList<>();
        caminhosAltTam = new ArrayList<>();

        caminhoAux = new int[maxDist + 1];
        caminhoAuxTam = 0;
        distanciaM = maxDist;
        custoFind = 999999.5;
        encontrou = false; // cuidado. Pode pegar o primeiro caminho que encontrar
        contArestas = arestas;
    }

//quer encontrar nó FIND. recebe lista de nós para procurar.
    public void searchWay(int find, int atual, int[] path, short distancia, double custo) {

//System.out.println("searchWay("+find+", "+atual+", .., "+distancia+")");
        if (distancia >= distanciaM || custo >= custoFind * fatorFind) /*||distancia>nos[atual] )*/ {
            return;
        }

        int[] path2 = new int[maxDist + 1];// = path;
        System.arraycopy(path, 0, path2, 0, distancia); // para que path seja por valor, e não referência/ponteiro

        if (nos[atual] > custo) // ???????????????????????????????????????????????
        {
            nos[atual] = custo; //nós = distancia do nó 
        } else {
            return;//já existe caminho mais curto para esse nó
        }
        path2[distancia] = atual; //acrescenta nó atual ao caminho
        ArrayList<Integer> vizinhos = getMape().getVizinhosIndexDoNode(atual, usarGemeos);

//vê se está na vizinhança;
        for (int a = 0; a < vizinhos.size(); a++) {
            if ((vizinhos.get(a)) == find) { //se há um vizinho que é o destino
//se estiver, retorna caminho até aqui
//distanciaM = distancia;
                caminhoAuxTam = distancia + 2;
                path2[distancia + 1] = find;
                caminhoAux = path2;
                if (custo + BPR_calcAvgTravelTimeInLinkA(atual, find) < custoFind) {
                    custoFind = custo + BPR_calcAvgTravelTimeInLinkA(atual, find);
                }

                caminhosAlt.add(path2);
                caminhosAltTam.add(caminhoAuxTam);
                custosAlt.add(custoFind);

// encontrou = true;
                return;
            }
        }

//se não estiver, indica próximos nós a buscar
        for (int ab = 0; ab < vizinhos.size(); ab++) {
//System.out.println(ab + " de "+ mape.getContVizinhosNode(atual, usarGemeos) +" = "+vizinhos.get(ab));
            searchWay(find, vizinhos.get(ab), path2, (short) (distancia + 1),
                    custo + BPR_calcAvgTravelTimeInLinkA(atual, vizinhos.get(ab)));

        }

    }

//fim de gerador de paths
    public geradordeRede2() {

        producao = new ArrayList<>();
        atracao = new ArrayList<>();
        latBorda = new ArrayList<>();
        lonBorda = new ArrayList<>();
        noProxBorda = new ArrayList<>();

        setMatrizPriori(new ArrayList<>());
        tamanhoCaminhos = new ArrayList<>();
        latNo = new ArrayList<>();
        lonNo = new ArrayList<>();
        codNo = new ArrayList<>();

        volDelayFunction1Hum = new ArrayList<>();
        volDelayFunction2Dois = new ArrayList<>();
        tempoLinkLivreEmSeg = new ArrayList<>();
        fromNo = new ArrayList<>();
        toNo = new ArrayList<>();
        tipoLink = new ArrayList<>();
        fromNoC = new ArrayList<>();
        toNoC = new ArrayList<>();
        capacidade = new ArrayList<>();
        capacidadeSemaforo = new ArrayList<>();
        contLink = new ArrayList<>();
        velocidade = new ArrayList<>();

        cadastrarMatrizPriori();
//construir mapa
//nós
        cadastrarNos();
//arestas
        cadastrarArestas();

        for (int a = 0; a < contLink.size(); a++) {
            if (contLink.get(a) > 0) {
                linksComContadores++;
            }
        }

        cadastrarAtracaoRepulsao();
        geh = new double[fromNoC.size()];
        cadastrarGEhLinks();

        System.out.println("RELAT: geradorDeRede ok; numPontosOD = " + numPontosOD + "; " + codNo.size() + " nós; " + fromNoC.size() + " arestas; linksComContadores = " + linksComContadores);

    }

    public double BPR_calcAvgTravelTimeInLinkA(int from, int to) {
        double resp;

        if (getMape() == null) {
            System.out.println("ERROR: geradorDeRede2.mape == null");
        }

        int linkA = getMape().getArestaIndex(from, to);
        if (linkA < 0) {
            System.out.println("ERROR: Nao encontrou aresta " + from + "to" + to + " (geradorDeRede.BPR_calc)");
        }
        double flow = this.fluxosParaBPR[linkA];

        resp = (flow / capacidadeSemaforo.get(linkA));
        resp = resp * resp * resp * resp;

        resp = tempoLinkLivreEmSeg.get(linkA) * (1 + 0.15 * resp);
// System.out.println(resp+" "+from+"to"+to+ "("+linkA+") f="+flow+" tLivr="+tempoLinkLivreEmSeg.get(linkA)+"; capac="+capacidadeSemaforo.get(linkA));
        return resp;
    }

    public String getIndexNoDoPontoOD(int x) {

        return getCodNodeMaisProximo(latBorda.get(x), lonBorda.get(x));
    }

    public String getCodNodeMaisProximo(double lat, double lon) {
        double dist = 9999999.0;
        int chosOne = 0;

        for (int x = 0; x < codNo.size(); x++) {
            if (distanciaPontos(lat, lon, latNo.get(x), lonNo.get(x)) < dist) {
                chosOne = x;
                dist = distanciaPontos(lat, lon, latNo.get(x), lonNo.get(x));
            }

        }

        return codNo.get(chosOne);
    }

    public double distanciaPontos(double x1, double y1, double x2, double y2) {

        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));

    }

    public ArrayList<taxi_Trip_Instance> getCaminhosOD(Mapping maps, VirtualSensors v) {
        ArrayList<Integer> nos;
        setMape(maps);

        String t;
        ArrayList<taxi_Trip_Instance> cam = new ArrayList<>();
        for (int x = 0; x < numPontosOD; x++) {
            for (int y = 0; y < numPontosOD; y++) {
                nos = new ArrayList<>();
                getMape().getNodeIndex(getIndexNoDoPontoOD(x));
                getMape().getNodeIndex(getIndexNoDoPontoOD(y));
//t = "parOD["+x+"]["+y+"]: De "+nos.get(0)+" para "+nos.get(1)+"; ";
                cam.add(new taxi_Trip_Instance(nos, getMape(), 5, false, false, false));
//System.out.println(t + "Caminho de "+ cam.get(cam.size()-1).getPosNodesEmMapa().size()+" nos!");
                tamanhoCaminhos.add(cam.get(cam.size() - 1).getPosNodesEmMapa().size());
            }
        }

        printStats(tamanhoCaminhos, "TamanhoCaminho");
        return cam;
    }

    public void printStats(ArrayList<Integer> ns, String name) {
        DecimalFormat df2 = new DecimalFormat(".##");
        DescriptiveStatistics d = new DescriptiveStatistics();
        for (int x = 0; x < ns.size(); x++) {
            d.addValue(ns.get(x));
        }

        String t = "STATS: " + name + ": Min. " + df2.format(d.getMin()) + "; Mean. " + df2.format(d.getMean()) + "; Max. " + df2.format(d.getMax()) + "; ";

        for (int p = 5; p < 100; p = p + 15) {
            t = t + "p" + p + " " + df2.format(d.getPercentile(p)) + "; ";
        }

        System.out.println(t);
    }

    public int getContNodes() {
        return getCodNo().size();
    }

    public int getContArestas() {
        return fromNoC.size();
    }

    public void addNo(int cod, double lat, double lon) {
        getCodNo().add("n" + cod);
        getLatNo().add(lat);
        getLonNo().add(lon);
    }

    public void testarMODSemente() {
        //contLink
        //matrizPriori
        //doParOD, probODaresta,
        System.out.println("TESTE MOD semente:");
        double[] estm = new double[contLink.size()];
        String[] sum = new String[contLink.size()];

        for (int o = 0; o < this.contLink.size(); o++) {
            sum[o] = "";
        }

        for (int o = 0; o < this.doParOD.size(); o++) {
            estm[daAresta.get(o)] = probODaresta.get(o) * matrizPriori.get(doParOD.get(o));
            sum[daAresta.get(o)] = sum[daAresta.get(o)] + " + p" + doParOD.get(o) + "*" + ((int) (100 * probODaresta.get(o))) / 100;
        }

        for (int o = 0; o < this.contLink.size(); o++) {
            System.out.println("Link " + o + " (" + fromNoC.get(o) + " t " + toNoC.get(o) + ") obs=" + contLink.get(o) + ";est=" + estm[o] + "; Eq = " + sum[o]);
        }

    }

    public void addLink(int from, int to, int tipoLink1, int capac, int capacSemaforo, int contagem,
            int velocidadex, int vdf1, int vdf2, double tempoLinkLivreEmS) {

        if (capacSemaforo < 1) {
            capacSemaforo = capac;
        }

        fromNoC.add("n" + from);
        toNoC.add("n" + to);
        fromNo.add(getNodeIndex("n" + from));
        toNo.add(getNodeIndex("n" + to));

        tipoLink.add(tipoLink1);
        capacidade.add(capac);
        capacidadeSemaforo.add(capacSemaforo);
        contLink.add(contagem);
        velocidade.add(velocidadex);
        volDelayFunction1Hum.add(((double) vdf1) / 100.0);
        volDelayFunction2Dois.add(((double) vdf2) / 100.0);
        tempoLinkLivreEmSeg.add((tempoLinkLivreEmS));
    }

    public int getNodeIndex(String c) {
        for (int n = 0; n < codNo.size(); n++) {
            if (codNo.get(n).equals(c)) {
                return n;
            }
        }
        System.out.println("ERROR: Não achou nó cod " + c + " entre os " + codNo.size() + " nós!");
        return -1;
    }

    public void addBorda(double lat, double lon, int produc, int atrac) {
        latBorda.add(lat);
        lonBorda.add(lon);
        producao.add(produc);
        atracao.add(atrac);

//noProxBorda.add(encontrarNoMaisProx(lat, lon));
        noProxBorda.add(findNoBorda(lat, lon, produc, atrac));
    }

    private int findNoBorda(double lat, double lon, int produ, int atrac) {
        int n = -1;
        if (produ > 0) {
            for (int c = 0; c < contLink.size(); c++) {
                if (contLink.get(c) == produ) {

                    if (n != -1 && n != fromNo.get(c)) {
                        System.out.println("DOUBT?: prod=" + produ + "; atrac=" + atrac + "; " + n + " ou " + fromNo.get(c) + " ? (gerador.findNoBorda)");
                    }

                    n = fromNo.get(c);
                }
            }
        }
        if (n == -1 && atrac > 0) {
            for (int c = 0; c < contLink.size(); c++) {
                if (contLink.get(c) == atrac) {

                    if (n != -1 && n != toNo.get(c)) {
                        System.out.println("DOUBT?: prod=" + produ + "; atrac=" + atrac + "; " + n + " ou " + toNo.get(c) + " ? (gerador.findNoBorda)");
                    }

                    n = toNo.get(c);
                }
            }
        }
        if (n == -1) {
            System.out.println("ERROR: Não encontrou borda com prod=" + produ + "; atrac=" + atrac + "; (gerador.findNoBorda)");
            n = encontrarNoMaisProx(lat, lon);
        }

        return n;
    }

    private int encontrarNoMaisProx(double lat, double lon) {
        int x = -1;
        double menorDist = 99999.0;

        for (int a = 0; a < latNo.size(); a++) {
            if (distanciaPontosMETROS(lat, lon, latNo.get(a), lonNo.get(a)) < menorDist) {
                x = a;
                menorDist = distanciaPontosMETROS(lat, lon, latNo.get(a), lonNo.get(a));
            }
        }
        System.out.println("Borda " + lat + ", " + lon + ": " + menorDist + "m");
        return x;
    }

    private double LOCAL_PI = 3.1415926535897932385;

    private double ToRadians(double degrees) {
        double radians = degrees * LOCAL_PI / 180;
        return radians;
    }

    public double distanciaPontosMETROS(double lat1, double lng1, double lat2, double lng2) {
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

    private void a(double x) {
        matrizPriori.add(x);
        somaMatrixPriori += x;
    }

    double[] estimado = new double[71];
    double[] observado = new double[71];

    public void addLinkGeh(int from, int to, double valor) {

        for (int x = 0; x < getFromNoC().size(); x++) {
            if (getFromNoC().get(x).equals("n" + from)) {
                if (getToNoC().get(x).equals("n" + to)) {
                    geh[x] = valor;
                    x = getFromNoC().size();
                }
            }
        }

    }

    private void cadastrarGEhLinks() {

        addLinkGeh(2, 323, 3.32741);
        addLinkGeh(323, 2, 14.670393);
        addLinkGeh(7, 8, 0.604508);
        addLinkGeh(41, 42, 6.179263);
        addLinkGeh(29, 516, 9.309338);
        addLinkGeh(29, 111, 2.949115);
        addLinkGeh(302, 242, 7.506342);
        addLinkGeh(117, 243, 9.814345);
        addLinkGeh(139, 306, 3.517903);
        addLinkGeh(138, 31, 1.477462);
        addLinkGeh(173, 144, 1.433881);
        addLinkGeh(155, 45, 1.595047);
        addLinkGeh(283, 45, 0.617073);
        addLinkGeh(45, 196, 4.740228);
        addLinkGeh(65, 45, 1.231486);
        addLinkGeh(285, 116, 1.777268);
        addLinkGeh(287, 120, 0.407202);
        addLinkGeh(304, 294, 1.966219);
        addLinkGeh(386, 305, 1.757419);
        addLinkGeh(2, 240, 1.09154);
        addLinkGeh(46, 66, 2.403057);
        addLinkGeh(315, 250, 4.243696);
        addLinkGeh(331, 130, 3.971071);
        addLinkGeh(333, 324, 1.145081);
        addLinkGeh(325, 327, 1.623536);
        addLinkGeh(125, 331, 20.600727);
        addLinkGeh(331, 128, 23.820413);
        addLinkGeh(358, 231, 7.975256);
        addLinkGeh(362, 332, 5.789014);
        addLinkGeh(130, 176, 3.165084);
        addLinkGeh(357, 350, 1.398366);
        addLinkGeh(355, 203, 1.810202);
        addLinkGeh(218, 341, 1.52486);
        addLinkGeh(356, 357, 1.474223);
        addLinkGeh(347, 360, 1.162534);
        addLinkGeh(358, 330, 4.812916);
        addLinkGeh(360, 361, 0.86959);
        addLinkGeh(344, 326, 0.961184);
        addLinkGeh(362, 338, 6.577175);
        addLinkGeh(366, 356, 0.438375);
        addLinkGeh(202, 376, 2.321247);
        addLinkGeh(382, 102, 0.37645);
        addLinkGeh(314, 196, 3.149251);
        addLinkGeh(387, 40, 1.716778);
        addLinkGeh(115, 241, 0.008382);
        addLinkGeh(311, 382, 3.918333);
        addLinkGeh(11, 292, 1.519958);
        addLinkGeh(262, 369, 2.178905);
        addLinkGeh(167, 341, 0.626865);
        addLinkGeh(516, 119, 9.309338);
        addLinkGeh(447, 354, 1.681153);
        addLinkGeh(369, 475, 0.624365);
        addLinkGeh(31, 114, 6.364573);
        addLinkGeh(81, 308, 0.258337);
        addLinkGeh(199, 38, 10.203913);
        addLinkGeh(329, 325, 14.663101);
        addLinkGeh(312, 324, 3.077141);
        addLinkGeh(316, 325, 2.887952);
        addLinkGeh(312, 251, 9.027693);
        addLinkGeh(316, 317, 1.17345);
        addLinkGeh(501, 502, 0.734003);
        addLinkGeh(503, 475, 0.77563);
        addLinkGeh(319, 518, 9.730834);
        addLinkGeh(518, 338, 20.184928);
        addLinkGeh(339, 330, 12.376067);
        addLinkGeh(674, 76, 9.745067);
        addLinkGeh(527, 675, 1.058803);
        addLinkGeh(114, 676, 4.42458);
        addLinkGeh(677, 678, 3.464102);
        addLinkGeh(126, 679, 2.201841);
        addLinkGeh(679, 126, 1.311769);

        int cont = 0;
        int menor5 = 0;
        double v = 0;

        for (int a = 0; a < getGeh().length; a++) {
            if (getGeh()[a] > 0) {
                v += getGeh()[a];
                cont++;
                if (getGeh()[a] < 5) {
                    menor5++;
                }
            }
        }

        System.out.println("INFO: Valor medio de GEH = " + (v / cont) + " (" + ((double) menor5 / cont) + " perc < 5.0)");

        Double geh[] = new Double[3];
        geh[0] = (v / cont);
        geh[1] = (v / cont);
        geh[2] = (v / cont);
        Double pgeh[] = new Double[3];
        pgeh[0] = ((double) menor5 / cont);
        pgeh[1] = ((double) menor5 / cont);
        pgeh[2] = ((double) menor5 / cont);
        Double r2L[] = new Double[3];
        r2L[0] = this.cadEstObs();
        r2L[1] = r2L[0];
        r2L[2] = r2L[0];

        resultsAlgoritmos rest = new resultsAlgoritmos();
        rest = rest.recuperarArquivo();
        Double vet[] = new Double[3];
        vet[0] = 0.0;
        vet[1] = 0.0;
        vet[2] = 0.0;
        double[] respMOD = new double[1];
        rest.addResultados("TFlowFuzzy", 0, vet, vet, vet, geh,
                vet, vet, vet, geh, respMOD, "", r2L, vet, pgeh, vet, vet);

        //( nome,                nSens,rsme2,mae2,fit, geh2,
        //    Double[] rsmeIn, Double[] maeIn, Double[] fitnessIn, Double[] gehIn, double[] mod, String tempo, Double[] r2l, Double[] r2od, Double [] percGeh) {
        rest.salvarArquivo(true);   //gera boxplot ao salvar arquivo

    }

    private void cadastrarAtracaoRepulsao() {
        addBorda(-49.2715489678, -25.4431889223, 0, 1486);
        addBorda(-49.2705584205, -25.4427822766, 895, 0);
        addBorda(-49.2685877527, -25.4420523996, 0, 1590);
        addBorda(-49.2668360481, -25.4413955104, 709, 0);
        addBorda(-49.2645318491, -25.4403742032, 211, 294);
        addBorda(-49.2623843192, -25.4388968722, 0, 1907);
        addBorda(-49.2608428191, -25.4383029609, 1926, 0);
        addBorda(-49.2599085767, -25.4375755864, 1304, 1761);
        addBorda(-49.2581809336, -25.4343803957, 189, 1968);
        addBorda(-49.2574858023, -25.4324122283, 2450, 0);
        addBorda(-49.2580413429, -25.4304286313, 0, 1496);
        addBorda(-49.2608718252, -25.4298145702, 0, 774);
        addBorda(-49.2624879813, -25.4304792796, 0, 2055);
        addBorda(-49.2633072208, -25.4307632162, 1849, 0);
        addBorda(-49.2644562127, -25.4312051362, 0, 832);
        addBorda(-49.2659538796, -25.4317959832, 864, 0);
        addBorda(-49.268025009, -25.4326975374, 0, 245);
        addBorda(-49.2695577506, -25.4332814389, 0, 57);
        addBorda(-49.2707582127, -25.4337121483, 0, 1247);
        addBorda(-49.2714476079, -25.4339419467, 511, 0);
        addBorda(-49.2721988719, -25.4341982603, 1037, 0);
        addBorda(-49.2751312734, -25.4355710368, 1923, 0);
        addBorda(-49.2758152724, -25.4376480581, 1164, 0);
        addBorda(-49.2749915541, -25.4392892385, 0, 3347);
        addBorda(-49.2743033844, -25.4405821634, 690, 767);
        addBorda(-49.2736256415, -25.4421149049, 2520, 0);
        addBorda(-49.2662051374, -25.4382096262, 421, 358);
        addBorda(-49.2616958914, -25.4302598077, 213, 0);
        addBorda(-49.2567074305, -25.4356457188, 200, 876);

    }

    private void cadastrarArestas() {

        addLink(302, 242, 32, 2036, 1063, 2123, 60, 43, 596, 2.932974);
        addLink(130, 176, 32, 1387, 493, 2080, 60, 43, 596, 10.78774332);
        addLink(331, 130, 43, 2427, 1133, 2062, 60, 43, 596, 10.22523138);
        addLink(114, 676, 32, 2080, 1109, 2021, 60, 43, 596, 7.25696274);
        addLink(315, 250, 32, 1732, 769, 2011, 60, 43, 596, 4.10170206);
        addLink(115, 241, 32, 1603, 659, 1985, 60, 43, 596, 4.54462932);
        addLink(46, 66, 32, 3900, 3900, 1928, 60, 43, 596, 6.96050694);
        addLink(202, 376, 42, 1950, 975, 1926, 60, 43, 596, 5.77936536);
        addLink(155, 45, 63, 2137, 878, 1923, 40, 60, 493, 14.31002223);
        addLink(369, 475, 63, 2542, 1243, 1922, 40, 60, 493, 6.64930296);
        addLink(527, 675, 32, 2080, 1109, 1915, 60, 43, 596, 7.0705851);
        addLink(355, 203, 43, 5200, 5200, 1907, 60, 43, 596, 5.4069339);
        addLink(387, 40, 42, 1993, 1018, 1892, 60, 43, 596, 6.43960494);
        addLink(312, 251, 32, 1732, 769, 1890, 60, 43, 596, 5.56918518);
        addLink(501, 502, 62, 3900, 3900, 1849, 40, 60, 493, 6.82937766);
        addLink(2, 240, 32, 1603, 659, 1812, 60, 43, 596, 2.94853536);
        addLink(316, 325, 43, 1560, 468, 1780, 60, 43, 596, 8.07404934);
        addLink(173, 144, 31, 2600, 2600, 1761, 60, 43, 596, 3.9938313);
        addLink(117, 243, 32, 2036, 1063, 1736, 60, 43, 596, 3.75517764);
        addLink(41, 42, 32, 1560, 624, 1706, 60, 43, 596, 9.41320692);
        addLink(362, 332, 32, 1950, 975, 1705, 60, 43, 596, 12.65155164);
        addLink(357, 350, 32, 3900, 3900, 1590, 60, 43, 596, 9.99645288);
        addLink(358, 231, 32, 2080, 1109, 1516, 60, 43, 596, 5.00048766);
        addLink(386, 305, 40, 1300, 1300, 1496, 60, 43, 596, 6.88001982);
        addLink(360, 361, 43, 5200, 5200, 1486, 60, 43, 596, 9.7404861);
        addLink(312, 324, 32, 1127, 326, 1426, 60, 43, 596, 8.52913722);
        addLink(316, 317, 32, 3900, 3900, 1419, 60, 43, 596, 6.99112716);
        addLink(29, 111, 32, 3900, 3900, 1414, 60, 43, 596, 4.5742131);
        addLink(356, 357, 31, 1300, 650, 1403, 60, 43, 596, 12.68898438);
        addLink(218, 341, 31, 1387, 740, 1387, 60, 43, 596, 8.99235378);
        addLink(45, 196, 63, 1849, 658, 1327, 40, 60, 493, 9.85449429);
        addLink(285, 116, 41, 925, 329, 1324, 60, 43, 596, 9.29677056);
        addLink(31, 114, 43, 1269, 310, 1315, 60, 43, 596, 12.33078486);
        addLink(7, 8, 31, 2600, 2600, 1304, 60, 43, 596, 3.59868474);
        addLink(344, 326, 32, 1863, 890, 1264, 60, 43, 596, 6.67572666);
        addLink(347, 360, 31, 1242, 593, 1256, 60, 43, 596, 6.68583294);
        addLink(382, 102, 61, 1097, 463, 1247, 40, 60, 493, 15.16629798);
        addLink(139, 306, 62, 2168, 1205, 1167, 40, 60, 493, 7.08512427);
        addLink(262, 369, 61, 2600, 2600, 1037, 40, 60, 493, 14.60343987);
        addLink(29, 516, 32, 3900, 3900, 1036, 60, 43, 596, 4.60065402);
        addLink(516, 119, 32, 1863, 890, 1036, 60, 43, 596, 5.65939326);
        addLink(287, 120, 31, 1040, 416, 967, 60, 43, 596, 9.3886119);
        addLink(366, 356, 42, 1732, 769, 895, 60, 43, 596, 9.60500964);
        addLink(199, 38, 61, 1446, 804, 888, 40, 60, 493, 13.60017);
        addLink(362, 338, 42, 997, 255, 872, 60, 43, 596, 9.44478894);
        addLink(138, 31, 63, 1907, 699, 864, 40, 60, 493, 14.071824);
        addLink(331, 128, 44, 800, 240, 862, 30, 76, 468, 17.02485252);
        addLink(358, 330, 31, 722, 201, 843, 60, 43, 596, 9.87915636);
        addLink(81, 308, 61, 2600, 2600, 832, 40, 60, 493, 7.81984719);
        addLink(304, 294, 51, 1186, 541, 774, 60, 43, 596, 8.60861274);
        addLink(325, 327, 44, 1300, 1300, 767, 30, 76, 468, 13.56505356);
        addLink(125, 331, 44, 800, 222, 763, 30, 76, 468, 24.19344492);
        addLink(518, 338, 44, 592, 270, 757, 30, 76, 468, 13.42760904);
        addLink(339, 330, 44, 477, 175, 748, 30, 76, 468, 22.23301128);
        addLink(311, 382, 31, 780, 234, 726, 60, 43, 596, 11.89711074);
        addLink(447, 354, 31, 1097, 463, 709, 60, 43, 596, 10.12426962);
        addLink(319, 518, 44, 737, 418, 690, 30, 76, 468, 13.165251);
        addLink(329, 325, 44, 737, 418, 682, 30, 76, 468, 13.4438364);
        addLink(283, 45, 41, 693, 185, 582, 60, 43, 596, 8.214225);
        addLink(65, 45, 41, 693, 185, 582, 60, 43, 596, 7.44132198);
        addLink(674, 76, 44, 809, 503, 578, 30, 76, 468, 14.62161804);
        addLink(314, 196, 41, 837, 270, 523, 60, 43, 596, 11.92287834);
        addLink(503, 475, 62, 909, 212, 511, 40, 60, 493, 14.97095424);
        addLink(679, 126, 44, 1300, 1300, 421, 30, 76, 468, 4.09101924);
        addLink(126, 679, 44, 1300, 1300, 358, 30, 76, 468, 4.09101924);
        addLink(333, 324, 44, 534, 219, 328, 30, 76, 468, 21.93348696);
        addLink(2, 323, 40, 549, 232, 245, 60, 43, 596, 8.56895346);
        addLink(167, 341, 41, 866, 288, 211, 60, 43, 596, 9.8346939);
        addLink(323, 2, 41, 635, 155, 187, 60, 43, 596, 8.56895346);
        addLink(11, 292, 41, 2600, 2600, 127, 60, 43, 596, 6.43838172);
        addLink(677, 678, 70, 433, 144, 6, 50, 43, 596, 11.465819784);
        addLink(1, 323, 40, 1300, 1300, 0, 60, 43, 596, 0.82957698);
        addLink(323, 1, 40, 1300, 1300, 0, 60, 43, 596, 0.82957698);
        addLink(6, 9, 34, 6500, 6500, 0, 60, 43, 596, 3.74221098);
        addLink(9, 6, 34, 3684, 2088, 0, 60, 43, 596, 3.74221098);
        addLink(8, 7, 31, 2600, 2600, 0, 60, 43, 596, 3.59868474);
        addLink(9, 10, 39, 1300, 1300, 0, 40, 60, 493, 6.11684811);
        addLink(10, 9, 39, 1300, 1300, 0, 40, 60, 493, 6.11684811);
        addLink(31, 32, 63, 2427, 1133, 0, 40, 60, 493, 13.33996686);
        addLink(32, 31, 63, 5200, 5200, 0, 40, 60, 493, 13.33996686);
        addLink(39, 40, 42, 1387, 493, 0, 60, 43, 596, 7.56465606);
        addLink(40, 39, 52, 5400, 5400, 0, 60, 43, 596, 7.56465606);
        addLink(42, 41, 32, 3900, 3900, 0, 60, 43, 596, 9.41320692);
        addLink(45, 46, 43, 2369, 1079, 0, 60, 43, 596, 11.95109808);
        addLink(46, 45, 43, 5200, 5200, 0, 60, 43, 596, 11.95109808);
        addLink(516, 29, 32, 3900, 3900, 0, 60, 43, 596, 4.60065402);
        addLink(111, 29, 32, 3900, 3900, 0, 60, 43, 596, 4.5742131);
        addLink(75, 76, 50, 317, 77, 0, 60, 43, 596, 8.76636864);
        addLink(76, 75, 51, 693, 185, 0, 60, 43, 596, 8.76636864);
        addLink(81, 138, 61, 300, 110, 0, 15, 43, 596, 44.11833288);
        addLink(138, 81, 61, 2600, 2600, 0, 40, 60, 493, 16.54437483);
        addLink(111, 293, 32, 1863, 890, 0, 60, 43, 596, 5.64725016);
        addLink(293, 111, 30, 1300, 1300, 0, 60, 43, 596, 5.64725016);
        addLink(293, 510, 32, 3900, 3900, 0, 60, 43, 596, 2.8165494);
        addLink(510, 293, 32, 3900, 3900, 0, 60, 43, 596, 2.8165494);
        addLink(112, 510, 30, 1300, 1300, 0, 60, 43, 596, 3.25316772);
        addLink(510, 112, 32, 3900, 3900, 0, 60, 43, 596, 3.25316772);
        addLink(75, 233, 32, 3900, 3900, 0, 60, 43, 596, 3.10577904);
        addLink(233, 75, 32, 2383, 1456, 0, 60, 43, 596, 3.10577904);
        addLink(122, 232, 31, 2600, 2600, 0, 60, 43, 596, 4.38496566);
        addLink(232, 122, 32, 2383, 1456, 0, 60, 43, 596, 4.38496566);
        addLink(115, 122, 31, 2600, 2600, 0, 60, 43, 596, 9.4167558);
        addLink(122, 115, 32, 2383, 1456, 0, 60, 43, 596, 9.4167558);
        addLink(42, 253, 32, 1473, 556, 0, 60, 43, 596, 3.56599944);
        addLink(253, 42, 31, 2600, 2600, 0, 60, 43, 596, 3.56599944);
        addLink(253, 302, 32, 1473, 556, 0, 60, 43, 596, 1.73581896);
        addLink(302, 253, 31, 2600, 2600, 0, 60, 43, 596, 1.73581896);
        addLink(242, 302, 31, 2600, 2600, 0, 60, 43, 596, 2.932974);
        addLink(116, 242, 31, 2600, 2600, 0, 60, 43, 596, 4.2149823);
        addLink(242, 116, 32, 2036, 1063, 0, 60, 43, 596, 4.2149823);
        addLink(243, 117, 31, 2600, 2600, 0, 60, 43, 596, 3.75517764);
        addLink(118, 243, 31, 2600, 2600, 0, 60, 43, 596, 3.40827762);
        addLink(243, 118, 32, 2036, 1063, 0, 60, 43, 596, 3.40827762);
        addLink(119, 511, 32, 3900, 3900, 0, 60, 43, 596, 4.4239284);
        addLink(511, 119, 30, 1300, 1300, 0, 60, 43, 596, 4.4239284);
        addLink(301, 511, 30, 1300, 1300, 0, 60, 43, 596, 1.63763898);
        addLink(511, 301, 32, 3900, 3900, 0, 60, 43, 596, 1.63763898);
        addLink(248, 301, 30, 1300, 1300, 0, 60, 43, 596, 2.0226999);
        addLink(301, 248, 32, 1170, 351, 0, 60, 43, 596, 2.0226999);
        addLink(120, 248, 30, 1300, 1300, 0, 60, 43, 596, 3.2024607);
        addLink(248, 120, 32, 1170, 351, 0, 60, 43, 596, 3.2024607);
        addLink(42, 120, 31, 2600, 2600, 0, 60, 43, 596, 1.01203992);
        addLink(120, 42, 32, 3900, 3900, 0, 60, 43, 596, 1.01203992);
        addLink(75, 122, 50, 1300, 1300, 0, 60, 43, 596, 0.7699749);
        addLink(122, 75, 50, 1300, 1300, 0, 60, 43, 596, 0.7699749);
        addLink(122, 123, 61, 549, 116, 0, 40, 60, 493, 17.88879303);
        addLink(123, 122, 61, 2600, 2600, 0, 40, 60, 493, 17.88879303);
        addLink(76, 124, 59, 1300, 1300, 0, 20, 43, 596, 4.80188106);
        addLink(124, 76, 59, 1300, 1300, 0, 20, 43, 596, 4.80188106);
        addLink(125, 126, 71, 200, 200, 0, 20, 43, 596, 34.15799412);
        addLink(126, 125, 51, 2600, 2600, 0, 60, 43, 596, 11.38599804);
        addLink(126, 127, 71, 200, 67, 0, 20, 43, 596, 12.6104733);
        addLink(127, 126, 51, 2600, 2600, 0, 60, 43, 596, 4.2034911);
        addLink(128, 129, 45, 800, 178, 0, 30, 76, 468, 8.30864616);
        addLink(129, 128, 41, 2600, 2600, 0, 60, 43, 596, 4.15432308);
        addLink(127, 216, 32, 1646, 695, 0, 60, 43, 596, 2.73184548);
        addLink(216, 127, 32, 3900, 3900, 0, 60, 43, 596, 2.73184548);
        addLink(178, 216, 32, 3900, 3900, 0, 60, 43, 596, 7.95239196);
        addLink(216, 178, 32, 1646, 695, 0, 60, 43, 596, 7.95239196);
        addLink(178, 220, 32, 1646, 695, 0, 60, 43, 596, 2.65263486);
        addLink(220, 178, 32, 3900, 3900, 0, 60, 43, 596, 2.65263486);
        addLink(220, 372, 32, 1646, 695, 0, 60, 43, 596, 1.5225165);
        addLink(372, 220, 32, 3900, 3900, 0, 60, 43, 596, 1.5225165);
        addLink(130, 372, 32, 3900, 3900, 0, 60, 43, 596, 0.68466846);
        addLink(372, 130, 32, 1646, 695, 0, 60, 43, 596, 0.68466846);
        addLink(131, 139, 62, 2168, 1205, 0, 40, 60, 493, 6.51547764);
        addLink(139, 131, 62, 3900, 3900, 0, 40, 60, 493, 6.51547764);
        addLink(306, 139, 62, 3900, 3900, 0, 40, 60, 493, 7.08512427);
        addLink(133, 134, 32, 2817, 2034, 0, 60, 43, 596, 1.8918561);
        addLink(134, 133, 32, 3900, 3900, 0, 60, 43, 596, 1.8918561);
        addLink(112, 303, 72, 2600, 2600, 0, 30, 76, 468, 26.08784832);
        addLink(303, 112, 72, 2600, 2600, 0, 30, 76, 468, 26.08784832);
        addLink(31, 138, 63, 5200, 5200, 0, 40, 60, 493, 14.071824);
        addLink(139, 502, 70, 300, 300, 0, 50, 43, 596, 5.963906592);
        addLink(502, 139, 70, 1300, 1300, 0, 50, 43, 596, 5.963906592);
        addLink(81, 502, 61, 2600, 2600, 0, 40, 60, 493, 10.89631233);
        addLink(502, 81, 61, 300, 93, 0, 15, 43, 596, 29.05683288);
        addLink(158, 252, 32, 1473, 556, 0, 60, 43, 596, 2.79124338);
        addLink(252, 158, 31, 2600, 2600, 0, 60, 43, 596, 2.79124338);
        addLink(117, 252, 31, 2600, 2600, 0, 60, 43, 596, 2.58356958);
        addLink(252, 117, 32, 1473, 556, 0, 60, 43, 596, 2.58356958);
        addLink(158, 160, 31, 2600, 2600, 0, 60, 43, 596, 0.94172676);
        addLink(160, 158, 32, 3900, 3900, 0, 60, 43, 596, 0.94172676);
        addLink(144, 173, 31, 2600, 2600, 0, 60, 43, 596, 3.9938313);
        addLink(41, 198, 44, 693, 369, 0, 30, 76, 468, 10.56491484);
        addLink(198, 41, 40, 1300, 1300, 0, 60, 43, 596, 5.28245742);
        addLink(176, 376, 35, 2774, 986, 0, 60, 43, 596, 0.60567972);
        addLink(376, 176, 35, 7800, 7800, 0, 60, 43, 596, 0.60567972);
        addLink(173, 376, 35, 7800, 7800, 0, 60, 43, 596, 3.8280741);
        addLink(376, 173, 35, 4072, 2126, 0, 60, 43, 596, 3.8280741);
        addLink(8, 173, 35, 7800, 7800, 0, 60, 43, 596, 1.95754806);
        addLink(173, 8, 35, 4243, 2308, 0, 60, 43, 596, 1.95754806);
        addLink(178, 179, 39, 1300, 1300, 0, 40, 60, 493, 2.2138767);
        addLink(179, 178, 39, 1300, 1300, 0, 40, 60, 493, 2.2138767);
        addLink(45, 155, 63, 5200, 5200, 0, 40, 60, 493, 14.31002223);
        addLink(45, 283, 41, 2600, 2600, 0, 60, 43, 596, 8.214225);
        addLink(196, 45, 63, 5200, 5200, 0, 40, 60, 493, 9.85449429);
        addLink(45, 65, 41, 2600, 2600, 0, 60, 43, 596, 7.44132198);
        addLink(198, 285, 44, 390, 117, 0, 30, 76, 468, 13.7882496);
        addLink(285, 198, 40, 1300, 1300, 0, 60, 43, 596, 6.8941248);
        addLink(121, 285, 40, 1300, 1300, 0, 60, 43, 596, 8.38573182);
        addLink(285, 121, 44, 607, 283, 0, 30, 76, 468, 16.77146364);
        addLink(160, 520, 31, 1242, 593, 0, 60, 43, 596, 7.13037126);
        addLink(520, 160, 31, 2600, 2600, 0, 60, 43, 596, 7.13037126);
        addLink(199, 520, 31, 2600, 2600, 0, 60, 43, 596, 5.59343382);
        addLink(520, 199, 31, 1242, 593, 0, 60, 43, 596, 5.59343382);
        addLink(120, 160, 31, 2600, 2600, 0, 60, 43, 596, 0.75362166);
        addLink(160, 120, 31, 2600, 2600, 0, 60, 43, 596, 0.75362166);
        addLink(158, 519, 32, 1863, 890, 0, 60, 43, 596, 6.13386132);
        addLink(519, 158, 32, 3900, 3900, 0, 60, 43, 596, 6.13386132);
        addLink(131, 519, 32, 3900, 3900, 0, 60, 43, 596, 6.59024082);
        addLink(519, 131, 32, 1863, 890, 0, 60, 43, 596, 6.59024082);
        addLink(213, 214, 49, 1300, 1300, 0, 20, 43, 596, 6.11112186);
        addLink(214, 213, 49, 1300, 1300, 0, 20, 43, 596, 6.11112186);
        addLink(215, 216, 39, 1300, 1300, 0, 40, 60, 493, 2.36574855);
        addLink(216, 215, 39, 1300, 1300, 0, 40, 60, 493, 2.36574855);
        addLink(217, 218, 39, 1300, 1300, 0, 40, 60, 493, 1.72989477);
        addLink(218, 217, 39, 1300, 1300, 0, 40, 60, 493, 1.72989477);
        addLink(219, 220, 39, 1300, 1300, 0, 40, 60, 493, 1.53939789);
        addLink(220, 219, 39, 1300, 1300, 0, 40, 60, 493, 1.53939789);
        addLink(221, 222, 39, 1300, 1300, 0, 40, 60, 493, 1.72869705);
        addLink(222, 221, 39, 1300, 1300, 0, 40, 60, 493, 1.72869705);
        addLink(221, 223, 39, 1300, 1300, 0, 40, 60, 493, 1.76253129);
        addLink(223, 221, 39, 1300, 1300, 0, 40, 60, 493, 1.76253129);
        addLink(228, 229, 39, 1300, 1300, 0, 40, 60, 493, 2.02756788);
        addLink(229, 228, 39, 1300, 1300, 0, 40, 60, 493, 2.02756788);
        addLink(230, 231, 39, 1300, 1300, 0, 40, 60, 493, 1.93236318);
        addLink(231, 230, 39, 1300, 1300, 0, 40, 60, 493, 1.93236318);
        addLink(232, 233, 39, 1300, 1300, 0, 40, 60, 493, 2.25550764);
        addLink(233, 232, 39, 1300, 1300, 0, 40, 60, 493, 2.25550764);
        addLink(238, 239, 39, 1300, 1300, 0, 40, 60, 493, 2.03613804);
        addLink(239, 238, 39, 1300, 1300, 0, 40, 60, 493, 2.03613804);
        addLink(240, 241, 39, 1300, 1300, 0, 40, 60, 493, 2.62104318);
        addLink(241, 240, 39, 1300, 1300, 0, 40, 60, 493, 2.62104318);
        addLink(242, 243, 39, 1300, 1300, 0, 40, 60, 493, 1.61942958);
        addLink(243, 242, 39, 1300, 1300, 0, 40, 60, 493, 1.61942958);
        addLink(244, 245, 39, 1300, 1300, 0, 40, 60, 493, 1.61012511);
        addLink(245, 244, 39, 1300, 1300, 0, 40, 60, 493, 1.61012511);
        addLink(246, 247, 40, 1300, 1300, 0, 60, 43, 596, 12.17097774);
        addLink(247, 246, 40, 1300, 1300, 0, 60, 43, 596, 12.17097774);
        addLink(248, 249, 39, 1300, 1300, 0, 40, 60, 493, 1.92423591);
        addLink(249, 248, 39, 1300, 1300, 0, 40, 60, 493, 1.92423591);
        addLink(250, 251, 39, 1300, 1300, 0, 40, 60, 493, 2.5773399);
        addLink(251, 250, 39, 1300, 1300, 0, 40, 60, 493, 2.5773399);
        addLink(252, 253, 39, 1300, 1300, 0, 40, 60, 493, 1.75668354);
        addLink(253, 252, 39, 1300, 1300, 0, 40, 60, 493, 1.75668354);
        addLink(256, 257, 39, 1300, 1300, 0, 40, 60, 493, 1.98355392);
        addLink(257, 256, 39, 1300, 1300, 0, 40, 60, 493, 1.98355392);
        addLink(258, 259, 39, 1300, 1300, 0, 40, 60, 493, 2.32230708);
        addLink(259, 258, 39, 1300, 1300, 0, 40, 60, 493, 2.32230708);
        addLink(2, 75, 32, 3900, 3900, 0, 60, 43, 596, 9.39638082);
        addLink(75, 2, 32, 2383, 1456, 0, 60, 43, 596, 9.39638082);
        addLink(116, 118, 41, 2600, 2600, 0, 60, 43, 596, 0.72382446);
        addLink(118, 116, 41, 2600, 2600, 0, 60, 43, 596, 0.72382446);
        addLink(10, 214, 42, 3900, 3900, 0, 60, 43, 596, 1.56780114);
        addLink(214, 10, 41, 2600, 2600, 0, 60, 43, 596, 1.56780114);
        addLink(214, 299, 42, 3900, 3900, 0, 60, 43, 596, 16.5024588);
        addLink(299, 214, 41, 2600, 2600, 0, 60, 43, 596, 16.5024588);
        addLink(259, 281, 75, 1300, 1300, 0, 4, 43, 596, 11.951226);
        addLink(281, 259, 75, 1300, 1300, 0, 4, 43, 596, 11.951226);
        addLink(41, 134, 32, 3900, 3900, 0, 60, 43, 596, 0.65867346);
        addLink(134, 41, 32, 3900, 3900, 0, 60, 43, 596, 0.65867346);
        addLink(134, 284, 42, 3900, 3900, 0, 60, 43, 596, 1.08618978);
        addLink(284, 134, 42, 3900, 3900, 0, 60, 43, 596, 1.08618978);
        addLink(128, 285, 42, 3900, 3900, 0, 60, 43, 596, 0.67779918);
        addLink(285, 128, 42, 3900, 3900, 0, 60, 43, 596, 0.67779918);
        addLink(116, 285, 41, 2600, 2600, 0, 60, 43, 596, 9.29677056);
        addLink(198, 472, 42, 3900, 3900, 0, 60, 43, 596, 0.79248516);
        addLink(472, 198, 42, 3900, 3900, 0, 60, 43, 596, 0.79248516);
        addLink(286, 472, 42, 3900, 3900, 0, 60, 43, 596, 1.5028377);
        addLink(472, 286, 42, 3900, 3900, 0, 60, 43, 596, 1.5028377);
        addLink(133, 284, 32, 2817, 2034, 0, 60, 43, 596, 1.90133604);
        addLink(284, 133, 31, 2600, 2600, 0, 60, 43, 596, 1.90133604);
        addLink(120, 287, 31, 2600, 2600, 0, 60, 43, 596, 9.3886119);
        addLink(41, 287, 41, 2600, 2600, 0, 60, 43, 596, 1.07720688);
        addLink(287, 41, 41, 2600, 2600, 0, 60, 43, 596, 1.07720688);
        addLink(129, 286, 45, 800, 178, 0, 30, 76, 468, 5.7207156);
        addLink(286, 129, 41, 2600, 2600, 0, 60, 43, 596, 2.8603578);
        addLink(11, 40, 52, 5400, 5400, 0, 60, 43, 596, 0.6726348);
        addLink(40, 11, 42, 3900, 3900, 0, 60, 43, 596, 0.6726348);
        addLink(287, 292, 41, 2600, 2600, 0, 60, 43, 596, 5.37540654);
        addLink(292, 287, 41, 433, 72, 0, 60, 43, 596, 5.37540654);
        addLink(119, 293, 51, 2600, 2600, 0, 60, 43, 596, 0.9196701);
        addLink(293, 119, 51, 2600, 2600, 0, 60, 43, 596, 0.9196701);
        addLink(293, 304, 51, 1502, 868, 0, 60, 43, 596, 12.9378336);
        addLink(304, 293, 51, 2600, 2600, 0, 60, 43, 596, 12.9378336);
        addLink(294, 304, 51, 2600, 2600, 0, 60, 43, 596, 8.60861274);
        addLink(292, 387, 72, 2600, 2600, 0, 30, 76, 468, 1.34596788);
        addLink(387, 292, 72, 2600, 2600, 0, 30, 76, 468, 1.34596788);
        addLink(297, 387, 72, 2600, 2600, 0, 30, 76, 468, 9.219726);
        addLink(387, 297, 72, 2600, 2600, 0, 30, 76, 468, 9.219726);
        addLink(112, 301, 72, 2600, 2600, 0, 30, 76, 468, 1.65268872);
        addLink(301, 112, 72, 2600, 2600, 0, 30, 76, 468, 1.65268872);
        addLink(292, 301, 72, 2600, 2600, 0, 30, 76, 468, 18.67459524);
        addLink(301, 292, 72, 2600, 2600, 0, 30, 76, 468, 18.67459524);
        addLink(198, 302, 42, 3900, 3900, 0, 60, 43, 596, 9.27092106);
        addLink(302, 198, 42, 1257, 405, 0, 60, 43, 596, 9.27092106);
        addLink(112, 249, 32, 1170, 351, 0, 60, 43, 596, 3.0548766);
        addLink(249, 112, 30, 1300, 1300, 0, 60, 43, 596, 3.0548766);
        addLink(160, 249, 30, 1300, 1300, 0, 60, 43, 596, 2.08031814);
        addLink(249, 160, 32, 1170, 351, 0, 60, 43, 596, 2.08031814);
        addLink(114, 527, 43, 5200, 5200, 0, 60, 43, 596, 0.78168384);
        addLink(527, 114, 43, 5200, 5200, 0, 60, 43, 596, 0.78168384);
        addLink(121, 527, 43, 5200, 5200, 0, 60, 43, 596, 9.05918178);
        addLink(527, 121, 43, 2137, 878, 0, 60, 43, 596, 9.05918178);
        addLink(131, 199, 63, 5200, 5200, 0, 40, 60, 493, 1.57826295);
        addLink(199, 131, 63, 5200, 5200, 0, 40, 60, 493, 1.57826295);
        addLink(199, 303, 40, 1300, 1300, 0, 60, 43, 596, 5.04099684);
        addLink(303, 199, 40, 1300, 1300, 0, 60, 43, 596, 5.04099684);
        addLink(303, 304, 40, 404, 126, 0, 60, 43, 596, 5.35843746);
        addLink(304, 303, 40, 1300, 1300, 0, 60, 43, 596, 5.35843746);
        addLink(304, 386, 40, 1300, 1300, 0, 60, 43, 596, 6.12683706);
        addLink(386, 304, 40, 1300, 1300, 0, 60, 43, 596, 6.12683706);
        addLink(305, 386, 40, 1300, 1300, 0, 60, 43, 596, 6.88001982);
        addLink(2, 115, 41, 2600, 2600, 0, 60, 43, 596, 0.82217664);
        addLink(115, 2, 40, 1300, 1300, 0, 60, 43, 596, 0.82217664);
        addLink(310, 311, 31, 2600, 2600, 0, 60, 43, 596, 0.73449822);
        addLink(311, 310, 31, 2600, 2600, 0, 60, 43, 596, 0.73449822);
        addLink(259, 310, 32, 3900, 3900, 0, 60, 43, 596, 5.25033876);
        addLink(310, 259, 32, 1213, 377, 0, 60, 43, 596, 5.25033876);
        addLink(259, 312, 32, 1213, 377, 0, 60, 43, 596, 5.11042644);
        addLink(312, 259, 32, 3900, 3900, 0, 60, 43, 596, 5.11042644);
        addLink(240, 2, 32, 3900, 3900, 0, 60, 43, 596, 2.94853536);
        addLink(313, 314, 41, 2600, 2600, 0, 60, 43, 596, 0.79685994);
        addLink(314, 313, 41, 2600, 2600, 0, 60, 43, 596, 0.79685994);
        addLink(66, 46, 31, 2600, 2600, 0, 60, 43, 596, 6.96050694);
        addLink(250, 315, 31, 2600, 2600, 0, 60, 43, 596, 4.10170206);
        addLink(250, 559, 32, 1732, 769, 0, 60, 43, 596, 2.3543967);
        addLink(559, 250, 31, 2600, 2600, 0, 60, 43, 596, 2.3543967);
        addLink(246, 559, 31, 2600, 2600, 0, 60, 43, 596, 0.45834462);
        addLink(559, 246, 32, 1732, 769, 0, 60, 43, 596, 0.45834462);
        addLink(245, 246, 31, 2600, 2600, 0, 60, 43, 596, 2.98309332);
        addLink(246, 245, 32, 1732, 769, 0, 60, 43, 596, 2.98309332);
        addLink(245, 314, 32, 1732, 769, 0, 60, 43, 596, 3.2352516);
        addLink(314, 245, 31, 2600, 2600, 0, 60, 43, 596, 3.2352516);
        addLink(257, 313, 32, 3900, 3900, 0, 60, 43, 596, 4.09096962);
        addLink(313, 257, 32, 1646, 695, 0, 60, 43, 596, 4.09096962);
        addLink(257, 316, 32, 1646, 695, 0, 60, 43, 596, 2.67797934);
        addLink(316, 257, 32, 3900, 3900, 0, 60, 43, 596, 2.67797934);
        addLink(312, 315, 33, 5200, 5200, 0, 60, 43, 596, 0.83217192);
        addLink(315, 312, 33, 5200, 5200, 0, 60, 43, 596, 0.83217192);
        addLink(46, 316, 43, 5200, 5200, 0, 60, 43, 596, 0.9217803);
        addLink(316, 46, 43, 5200, 5200, 0, 60, 43, 596, 0.9217803);
        addLink(121, 331, 43, 5200, 5200, 0, 60, 43, 596, 0.89300262);
        addLink(331, 121, 43, 5200, 5200, 0, 60, 43, 596, 0.89300262);
        addLink(130, 331, 43, 5200, 5200, 0, 60, 43, 596, 10.22523138);
        addLink(76, 572, 44, 607, 283, 0, 30, 76, 468, 1.73554332);
        addLink(572, 76, 40, 1300, 1300, 0, 60, 43, 596, 0.86777166);
        addLink(572, 573, 44, 607, 283, 0, 30, 76, 468, 14.71813248);
        addLink(573, 572, 40, 1300, 1300, 0, 60, 43, 596, 7.35906624);
        addLink(323, 573, 40, 1300, 1300, 0, 60, 43, 596, 0.84962232);
        addLink(573, 323, 44, 607, 283, 0, 30, 76, 468, 1.69924464);
        addLink(323, 333, 44, 274, 58, 0, 30, 76, 468, 14.3667318);
        addLink(333, 323, 40, 1300, 1300, 0, 60, 43, 596, 7.1833659);
        addLink(324, 333, 40, 1300, 1300, 0, 60, 43, 596, 10.96674348);
        addLink(325, 518, 43, 5200, 5200, 0, 60, 43, 596, 1.24366194);
        addLink(518, 325, 43, 5200, 5200, 0, 60, 43, 596, 1.24366194);
        addLink(326, 518, 43, 5200, 5200, 0, 60, 43, 596, 9.72005508);
        addLink(518, 326, 43, 2137, 878, 0, 60, 43, 596, 9.72005508);
        addLink(327, 325, 40, 1300, 1300, 0, 60, 43, 596, 6.78252678);
        addLink(324, 329, 44, 260, 52, 0, 30, 76, 468, 25.34824068);
        addLink(329, 324, 40, 1300, 1300, 0, 60, 43, 596, 12.67412034);
        addLink(330, 574, 44, 809, 503, 0, 30, 76, 468, 16.01847036);
        addLink(574, 330, 40, 1300, 1300, 0, 60, 43, 596, 8.00923518);
        addLink(571, 574, 40, 1300, 1300, 0, 60, 43, 596, 7.30816044);
        addLink(574, 571, 44, 809, 503, 0, 30, 76, 468, 14.61632088);
        addLink(124, 571, 40, 1300, 1300, 0, 60, 43, 596, 1.52611098);
        addLink(571, 124, 44, 809, 503, 0, 30, 76, 468, 3.05222196);
        addLink(124, 125, 44, 361, 100, 0, 30, 76, 468, 4.45538688);
        addLink(125, 124, 40, 1300, 1300, 0, 60, 43, 596, 2.22769344);
        addLink(331, 125, 40, 1300, 1300, 0, 60, 43, 596, 12.09672246);
        addLink(128, 331, 41, 2600, 2600, 0, 60, 43, 596, 8.51242626);
        addLink(324, 339, 32, 3900, 3900, 0, 60, 43, 596, 1.24542588);
        addLink(339, 324, 32, 3900, 3900, 0, 60, 43, 596, 1.24542588);
        addLink(332, 339, 32, 3900, 3900, 0, 60, 43, 596, 9.73651026);
        addLink(339, 332, 32, 1473, 556, 0, 60, 43, 596, 9.73651026);
        addLink(330, 333, 31, 2600, 2600, 0, 60, 43, 596, 1.37123982);
        addLink(333, 330, 31, 2600, 2600, 0, 60, 43, 596, 1.37123982);
        addLink(310, 333, 31, 2600, 2600, 0, 60, 43, 596, 8.64461436);
        addLink(333, 310, 31, 722, 201, 0, 60, 43, 596, 8.64461436);
        addLink(338, 339, 44, 534, 219, 0, 30, 76, 468, 25.41044736);
        addLink(339, 338, 40, 1300, 1300, 0, 60, 43, 596, 12.70522368);
        addLink(329, 338, 42, 3900, 3900, 0, 60, 43, 596, 1.33758672);
        addLink(338, 329, 42, 3900, 3900, 0, 60, 43, 596, 1.33758672);
        addLink(127, 341, 51, 2600, 2600, 0, 60, 43, 596, 0.89060778);
        addLink(341, 127, 51, 2600, 2600, 0, 60, 43, 596, 0.89060778);
        addLink(221, 332, 32, 3900, 3900, 0, 60, 43, 596, 7.29363576);
        addLink(332, 221, 32, 1732, 769, 0, 60, 43, 596, 7.29363576);
        addLink(221, 358, 32, 1732, 769, 0, 60, 43, 596, 4.02140256);
        addLink(358, 221, 32, 3900, 3900, 0, 60, 43, 596, 4.02140256);
        addLink(231, 358, 32, 3900, 3900, 0, 60, 43, 596, 5.00048766);
        addLink(217, 231, 32, 3900, 3900, 0, 60, 43, 596, 0.99633306);
        addLink(231, 217, 32, 2080, 1109, 0, 60, 43, 596, 0.99633306);
        addLink(127, 217, 32, 3900, 3900, 0, 60, 43, 596, 9.73991172);
        addLink(217, 127, 32, 2080, 1109, 0, 60, 43, 596, 9.73991172);
        addLink(229, 326, 32, 3900, 3900, 0, 60, 43, 596, 4.35476766);
        addLink(326, 229, 32, 1732, 769, 0, 60, 43, 596, 4.35476766);
        addLink(229, 362, 32, 1732, 769, 0, 60, 43, 596, 2.5237278);
        addLink(362, 229, 32, 3900, 3900, 0, 60, 43, 596, 2.5237278);
        addLink(332, 362, 32, 3900, 3900, 0, 60, 43, 596, 12.65155164);
        addLink(176, 130, 32, 3900, 3900, 0, 60, 43, 596, 10.78774332);
        addLink(332, 357, 32, 3900, 3900, 0, 60, 43, 596, 0.9665955);
        addLink(357, 332, 32, 3900, 3900, 0, 60, 43, 596, 0.9665955);
        addLink(350, 357, 32, 3900, 3900, 0, 60, 43, 596, 9.99645288);
        addLink(130, 355, 43, 5200, 5200, 0, 60, 43, 596, 0.8226099);
        addLink(355, 130, 43, 5200, 5200, 0, 60, 43, 596, 0.8226099);
        addLink(203, 355, 43, 5200, 5200, 0, 60, 43, 596, 5.4069339);
        addLink(230, 354, 31, 2600, 2600, 0, 60, 43, 596, 3.97236816);
        addLink(354, 230, 31, 1387, 740, 0, 60, 43, 596, 3.97236816);
        addLink(218, 230, 31, 2600, 2600, 0, 60, 43, 596, 2.7567528);
        addLink(230, 218, 31, 1387, 740, 0, 60, 43, 596, 2.567528);
        addLink(341, 218, 31, 2600, 2600, 0, 60, 43, 596, 8.99235378);
        addLink(215, 341, 31, 2600, 2600, 0, 60, 43, 596, 1.398345);
        addLink(341, 215, 31, 1097, 463, 0, 60, 43, 596, 1.398345);
        addLink(179, 215, 31, 2600, 2600, 0, 60, 43, 596, 10.39088346);
        addLink(215, 179, 31, 1097, 463, 0, 60, 43, 596, 10.39088346);
        addLink(179, 219, 31, 1097, 463, 0, 60, 43, 596, 1.1190672);
        addLink(219, 179, 31, 2600, 2600, 0, 60, 43, 596, 1.1190672);
        addLink(219, 371, 31, 1097, 463, 0, 60, 43, 596, 2.33985318);
        addLink(371, 219, 31, 2600, 2600, 0, 60, 43, 596, 2.33985318);
        addLink(355, 371, 31, 2600, 2600, 0, 60, 43, 596, 0.40172094);
        addLink(371, 355, 31, 1097, 463, 0, 60, 43, 596, 0.40172094);
        addLink(176, 355, 32, 3900, 3900, 0, 60, 43, 596, 10.69437294);
        addLink(355, 176, 32, 1387, 493, 0, 60, 43, 596, 10.69437294);
        addLink(357, 356, 31, 2600, 2600, 0, 60, 43, 596, 12.68898438);
        addLink(223, 357, 31, 2600, 2600, 0, 60, 43, 596, 6.39046506);
        addLink(357, 223, 31, 1154, 512, 0, 60, 43, 596, 6.39046506);
        addLink(222, 223, 31, 2600, 2600, 0, 60, 43, 596, 1.77673818);
        addLink(223, 222, 31, 1154, 512, 0, 60, 43, 596, 1.77673818);
        addLink(222, 354, 31, 1154, 512, 0, 60, 43, 596, 3.18624498);
        addLink(354, 222, 31, 2600, 2600, 0, 60, 43, 596, 3.18624498);
        addLink(360, 347, 31, 2600, 2600, 0, 60, 43, 596, 6.68583294);
        addLink(228, 360, 31, 2600, 2600, 0, 60, 43, 596, 3.44877222);
        addLink(360, 228, 31, 1154, 512, 0, 60, 43, 596, 3.44877222);
        addLink(228, 356, 31, 1154, 512, 0, 60, 43, 596, 3.38604642);
        addLink(356, 228, 31, 2600, 2600, 0, 60, 43, 596, 3.38604642);
        addLink(354, 358, 31, 2600, 2600, 0, 60, 43, 596, 0.75772296);
        addLink(358, 354, 31, 2600, 2600, 0, 60, 43, 596, 0.75772296);
        addLink(330, 358, 31, 2600, 2600, 0, 60, 43, 596, 9.87915636);
        addLink(326, 360, 43, 5200, 5200, 0, 60, 43, 596, 1.09212216);
        addLink(360, 326, 43, 5200, 5200, 0, 60, 43, 596, 1.09212216);
        addLink(361, 360, 43, 5200, 5200, 0, 60, 43, 596, 9.7404861);
        addLink(326, 344, 32, 3900, 3900, 0, 60, 43, 596, 6.67572666);
        addLink(356, 362, 42, 3900, 3900, 0, 60, 43, 596, 1.13142654);
        addLink(362, 356, 42, 3900, 3900, 0, 60, 43, 596, 1.13142654);
        addLink(338, 362, 42, 3900, 3900, 0, 60, 43, 596, 9.44478894);
        addLink(356, 366, 42, 3900, 3900, 0, 60, 43, 596, 9.60500964);
        addLink(371, 372, 75, 1300, 1300, 0, 4, 43, 596, 14.4241677);
        addLink(372, 371, 75, 1300, 1300, 0, 4, 43, 596, 14.4241677);
        addLink(376, 202, 42, 3900, 3900, 0, 60, 43, 596, 5.77936536);
        addLink(128, 376, 41, 2600, 2600, 0, 60, 43, 596, 11.3709843);
        addLink(376, 128, 41, 1473, 835, 0, 60, 43, 596, 11.3709843);
        addLink(39, 298, 40, 1300, 1300, 0, 60, 43, 596, 5.0982768);
        addLink(298, 39, 42, 3900, 3900, 0, 60, 43, 596, 5.0982768);
        addLink(196, 247, 63, 5200, 5200, 0, 40, 60, 493, 9.20597868);
        addLink(247, 196, 63, 5200, 5200, 0, 40, 60, 493, 9.20597868);
        addLink(102, 382, 61, 2600, 2600, 0, 40, 60, 493, 15.16629798);
        addLink(382, 383, 63, 2137, 878, 0, 40, 60, 493, 10.65798189);
        addLink(383, 382, 63, 5200, 5200, 0, 40, 60, 493, 10.65798189);
        addLink(111, 386, 72, 2600, 2600, 0, 30, 76, 468, 26.009802);
        addLink(386, 111, 72, 2600, 2600, 0, 30, 76, 468, 26.009802);
        addLink(196, 314, 41, 2600, 2600, 0, 60, 43, 596, 11.92287834);
        addLink(256, 314, 31, 2600, 2600, 0, 60, 43, 596, 2.95755564);
        addLink(314, 256, 32, 1646, 695, 0, 60, 43, 596, 2.95755564);
        addLink(46, 256, 31, 2600, 2600, 0, 60, 43, 596, 3.78069936);
        addLink(256, 46, 32, 1646, 695, 0, 60, 43, 596, 3.78069936);
        addLink(284, 387, 42, 3900, 3900, 0, 60, 43, 596, 5.4496596);
        addLink(387, 284, 42, 3900, 3900, 0, 60, 43, 596, 5.4496596);
        addLink(40, 387, 42, 3900, 3900, 0, 60, 43, 596, 6.43960494);
        addLink(284, 287, 31, 2600, 2600, 0, 60, 43, 596, 0.74410362);
        addLink(287, 284, 31, 2600, 2600, 0, 60, 43, 596, 0.74410362);
        addLink(115, 383, 42, 1170, 351, 0, 60, 43, 596, 11.99310378);
        addLink(383, 115, 42, 3900, 3900, 0, 60, 43, 596, 11.99310378);
        addLink(241, 115, 31, 2600, 2600, 0, 60, 43, 596, 4.54462932);
        addLink(241, 311, 32, 1603, 659, 0, 60, 43, 596, 2.76728778);
        addLink(311, 241, 31, 2600, 2600, 0, 60, 43, 596, 2.76728778);
        addLink(382, 311, 31, 2600, 2600, 0, 60, 43, 596, 11.89711074);
        addLink(258, 311, 31, 2600, 2600, 0, 60, 43, 596, 3.8905539);
        addLink(311, 258, 32, 1213, 377, 0, 60, 43, 596, 3.8905539);
        addLink(258, 281, 32, 1213, 377, 0, 60, 43, 596, 1.34904384);
        addLink(281, 258, 31, 2600, 2600, 0, 60, 43, 596, 1.34904384);
        addLink(281, 476, 32, 1213, 377, 0, 60, 43, 596, 0.39355776);
        addLink(476, 281, 31, 2600, 2600, 0, 60, 43, 596, 0.39355776);
        addLink(315, 476, 31, 2600, 2600, 0, 60, 43, 596, 4.6332465);
        addLink(476, 315, 32, 1213, 377, 0, 60, 43, 596, 4.6332465);
        addLink(292, 11, 41, 2600, 2600, 0, 60, 43, 596, 6.43838172);
        addLink(6, 133, 34, 4694, 3390, 0, 60, 43, 596, 1.88098794);
        addLink(133, 6, 33, 5200, 5200, 0, 60, 43, 596, 1.88098794);
        addLink(6, 10, 42, 3900, 3900, 0, 60, 43, 596, 1.45516284);
        addLink(10, 6, 41, 2600, 2600, 0, 60, 43, 596, 1.45516284);
        addLink(247, 369, 63, 2485, 1187, 0, 40, 60, 493, 10.5627717);
        addLink(369, 247, 63, 5200, 5200, 0, 40, 60, 493, 10.5627717);
        addLink(369, 262, 61, 2600, 2600, 0, 40, 60, 493, 14.60343987);
        addLink(341, 167, 40, 1300, 1300, 0, 60, 43, 596, 9.8346939);
        addLink(119, 516, 30, 1300, 1300, 0, 60, 43, 596, 5.65939326);
        addLink(354, 447, 31, 2600, 2600, 0, 60, 43, 596, 10.12426962);
        addLink(134, 472, 42, 3900, 3900, 0, 60, 43, 596, 5.18237922);
        addLink(472, 134, 45, 433, 72, 0, 30, 76, 468, 10.36475844);
        addLink(6, 286, 41, 2600, 2600, 0, 60, 43, 596, 5.27803554);
        addLink(286, 6, 42, 1170, 351, 0, 60, 43, 596, 5.27803554);
        addLink(129, 472, 45, 800, 178, 0, 30, 76, 468, 5.33264172);
        addLink(472, 129, 41, 2600, 2600, 0, 60, 43, 596, 2.66632086);
        addLink(315, 369, 33, 5200, 5200, 0, 60, 43, 596, 12.01143276);
        addLink(369, 315, 33, 1502, 434, 0, 60, 43, 596, 12.01143276);
        addLink(475, 369, 63, 5200, 5200, 0, 40, 60, 493, 6.64930296);
        addLink(475, 476, 41, 2600, 2600, 0, 60, 43, 596, 12.0048792);
        addLink(476, 475, 41, 2600, 2600, 0, 60, 43, 596, 12.0048792);
        addLink(382, 475, 63, 5200, 5200, 0, 40, 60, 493, 8.36182323);
        addLink(475, 382, 63, 2080, 832, 0, 40, 60, 493, 8.36182323);
        addLink(123, 383, 63, 5200, 5200, 0, 40, 60, 493, 14.34878316);
        addLink(383, 123, 63, 2658, 1359, 0, 40, 60, 493, 14.34878316);
        addLink(101, 383, 60, 1300, 1300, 0, 40, 60, 493, 14.87052567);
        addLink(383, 101, 60, 1300, 1300, 0, 40, 60, 493, 14.87052567);
        addLink(82, 123, 61, 2600, 2600, 0, 40, 60, 493, 14.63273559);
        addLink(123, 82, 61, 953, 349, 0, 40, 60, 493, 14.63273559);
        addLink(114, 31, 43, 5200, 5200, 0, 60, 43, 596, 12.33078486);
        addLink(32, 81, 61, 1040, 416, 0, 40, 60, 493, 6.25163031);
        addLink(81, 32, 61, 2600, 2600, 0, 40, 60, 493, 6.25163031);
        addLink(308, 81, 61, 2600, 2600, 0, 40, 60, 493, 7.81984719);
        addLink(32, 478, 63, 2309, 1025, 0, 40, 60, 493, 10.83496068);
        addLink(478, 32, 63, 5200, 5200, 0, 40, 60, 493, 10.83496068);
        addLink(117, 478, 42, 3900, 3900, 0, 60, 43, 596, 12.78034236);
        addLink(478, 117, 42, 1213, 377, 0, 60, 43, 596, 12.78034236);
        addLink(131, 478, 40, 1300, 1300, 0, 60, 43, 596, 5.10670428);
        addLink(478, 131, 40, 506, 197, 0, 60, 43, 596, 5.10670428);
        addLink(38, 199, 61, 2600, 2600, 0, 40, 60, 493, 13.60017);
        addLink(313, 329, 41, 2600, 2600, 0, 60, 43, 596, 8.44800498);
        addLink(329, 313, 41, 606, 141, 0, 60, 43, 596, 8.44800498);
        addLink(325, 329, 40, 1300, 1300, 0, 60, 43, 596, 6.7219182);
        addLink(11, 119, 42, 1560, 624, 0, 60, 43, 596, 9.39041562);
        addLink(119, 11, 52, 5400, 5400, 0, 60, 43, 596, 9.39041562);
        addLink(39, 297, 42, 3900, 3900, 0, 60, 43, 596, 7.54755846);
        addLink(297, 39, 40, 1300, 1300, 0, 60, 43, 596, 7.54755846);
        addLink(213, 297, 40, 1300, 1300, 0, 60, 43, 596, 3.28262562);
        addLink(297, 213, 42, 1170, 351, 0, 60, 43, 596, 3.28262562);
        addLink(133, 213, 40, 1300, 1300, 0, 60, 43, 596, 3.62535612);
        addLink(213, 133, 41, 780, 234, 0, 60, 43, 596, 3.62535612);
        addLink(324, 312, 32, 3900, 3900, 0, 60, 43, 596, 8.52913722);
        addLink(325, 316, 43, 5200, 5200, 0, 60, 43, 596, 8.07404934);
        addLink(251, 312, 32, 3900, 3900, 0, 60, 43, 596, 5.56918518);
        addLink(251, 560, 32, 1732, 769, 0, 60, 43, 596, 0.90914316);
        addLink(560, 251, 32, 3900, 3900, 0, 60, 43, 596, 0.90914316);
        addLink(244, 560, 32, 3900, 3900, 0, 60, 43, 596, 2.6121864);
        addLink(560, 244, 32, 1732, 769, 0, 60, 43, 596, 2.6121864);
        addLink(244, 313, 32, 1732, 769, 0, 60, 43, 596, 3.98019624);
        addLink(313, 244, 32, 3900, 3900, 0, 60, 43, 596, 3.98019624);
        addLink(317, 316, 31, 2600, 2600, 0, 60, 43, 596, 6.99112716);
        addLink(502, 501, 62, 3900, 3900, 0, 40, 60, 493, 6.82937766);
        addLink(478, 502, 62, 3900, 3900, 0, 40, 60, 493, 6.6458016);
        addLink(502, 478, 62, 1560, 624, 0, 40, 60, 493, 6.6458016);
        addLink(475, 503, 62, 3900, 3900, 0, 40, 60, 493, 14.97095424);
        addLink(118, 238, 32, 1560, 624, 0, 60, 43, 596, 4.4627352);
        addLink(238, 118, 31, 2600, 2600, 0, 60, 43, 596, 4.4627352);
        addLink(114, 238, 31, 2600, 2600, 0, 60, 43, 596, 4.0851066);
        addLink(238, 114, 32, 1560, 624, 0, 60, 43, 596, 4.0851066);
        addLink(32, 118, 61, 2600, 2600, 0, 40, 60, 493, 18.75543471);
        addLink(118, 32, 61, 635, 155, 0, 40, 60, 493, 18.75543471);
        addLink(510, 511, 39, 1300, 1300, 0, 40, 60, 493, 2.62943532);
        addLink(511, 510, 39, 1300, 1300, 0, 40, 60, 493, 2.62943532);
        addLink(111, 516, 72, 2600, 2600, 0, 30, 76, 468, 2.30478948);
        addLink(516, 111, 72, 2600, 2600, 0, 30, 76, 468, 2.30478948);
        addLink(518, 319, 40, 1300, 1300, 0, 60, 43, 596, 6.5826255);
        addLink(338, 518, 40, 1300, 1300, 0, 60, 43, 596, 6.71380452);
        addLink(330, 339, 40, 1300, 1300, 0, 60, 43, 596, 11.11650564);
        addLink(519, 520, 39, 1300, 1300, 0, 40, 60, 493, 2.1872844);
        addLink(520, 519, 39, 1300, 1300, 0, 40, 60, 493, 2.1872844);
        addLink(42, 158, 32, 3900, 3900, 0, 60, 43, 596, 0.75768576);
        addLink(158, 42, 32, 3900, 3900, 0, 60, 43, 596, 0.75768576);
        addLink(8, 9, 36, 5157, 2922, 0, 60, 43, 596, 3.77633436);
        addLink(9, 8, 36, 9100, 9100, 0, 60, 43, 596, 3.77633436);
        addLink(173, 286, 42, 3900, 3900, 0, 60, 43, 596, 9.12454404);
        addLink(286, 173, 42, 1342, 462, 0, 60, 43, 596, 9.12454404);
        addLink(240, 310, 32, 1603, 659, 0, 60, 43, 596, 4.27475664);
        addLink(310, 240, 32, 3900, 3900, 0, 60, 43, 596, 4.27475664);
        addLink(116, 239, 32, 1560, 624, 0, 60, 43, 596, 5.5555233);
        addLink(239, 116, 32, 3900, 3900, 0, 60, 43, 596, 5.5555233);
        addLink(239, 527, 32, 1560, 624, 0, 60, 43, 596, 2.96947458);
        addLink(527, 239, 32, 3900, 3900, 0, 60, 43, 596, 2.96947458);
        addLink(117, 302, 42, 3900, 3900, 0, 60, 43, 596, 0.72781776);
        addLink(302, 117, 42, 3900, 3900, 0, 60, 43, 596, 0.72781776);
        addLink(559, 560, 75, 1300, 1300, 0, 4, 43, 596, 12.4864218);
        addLink(560, 559, 75, 1300, 1300, 0, 4, 43, 596, 12.4864218);
        addLink(571, 572, 75, 1300, 1300, 0, 4, 43, 596, 21.2576418);
        addLink(572, 571, 75, 1300, 1300, 0, 4, 43, 596, 21.2576418);
        addLink(573, 574, 75, 1300, 1300, 0, 4, 43, 596, 21.1463091);
        addLink(574, 573, 75, 1300, 1300, 0, 4, 43, 596, 21.1463091);
        addLink(38, 306, 70, 1300, 1300, 0, 50, 43, 596, 1.151851968);
        addLink(306, 38, 70, 1300, 1300, 0, 50, 43, 596, 1.151851968);
        addLink(76, 674, 40, 1300, 1300, 0, 60, 43, 596, 7.31080902);
        addLink(121, 674, 44, 636, 311, 0, 30, 76, 468, 15.19730064);
        addLink(674, 121, 40, 1300, 1300, 0, 60, 43, 596, 7.59865032);
        addLink(233, 675, 32, 3900, 3900, 0, 60, 43, 596, 4.44052542);
        addLink(675, 233, 32, 2383, 1456, 0, 60, 43, 596, 4.44052542);
        addLink(675, 527, 32, 3900, 3900, 0, 60, 43, 596, 7.0705851);
        addLink(674, 675, 70, 419, 135, 0, 50, 43, 596, 10.452292488);
        addLink(675, 674, 70, 520, 208, 0, 50, 43, 596, 10.452292488);
        addLink(676, 114, 31, 2600, 2600, 0, 60, 43, 596, 7.25696274);
        addLink(232, 676, 31, 2600, 2600, 0, 60, 43, 596, 2.9433096);
        addLink(676, 232, 32, 2383, 1456, 0, 60, 43, 596, 2.9433096);
        addLink(675, 676, 70, 1300, 1300, 0, 50, 43, 596, 1.1439954);
        addLink(676, 675, 70, 1300, 1300, 0, 50, 43, 596, 1.1439954);
        addLink(31, 677, 63, 5200, 5200, 0, 40, 60, 493, 10.71973341);
        addLink(677, 31, 63, 1502, 434, 0, 40, 60, 493, 10.71973341);
        addLink(123, 677, 63, 2657, 1358, 0, 40, 60, 493, 11.49222564);
        addLink(677, 123, 63, 5200, 5200, 0, 40, 60, 493, 11.49222564);
        addLink(138, 678, 60, 300, 133, 0, 40, 60, 493, 10.26654786);
        addLink(678, 138, 60, 1300, 1300, 0, 40, 60, 493, 10.26654786);
        addLink(676, 677, 70, 433, 144, 0, 50, 43, 596, 14.563887552);
        addLink(677, 676, 70, 419, 135, 0, 50, 43, 596, 14.563887552);
        addLink(678, 677, 70, 433, 144, 0, 50, 43, 596, 11.465819784);
        addLink(323, 574, 40, 1800, 0, 0, 60, 43, 596, 1.68321072);
        addLink(574, 323, 41, 3600, 0, 0, 60, 43, 596, 1.68321072);
        addLink(303, 680, 61, 1600, 0, 0, 40, 60, 493, 13.16554632);
        addLink(680, 303, 61, 1600, 0, 0, 40, 60, 493, 13.16554632);

    }

    private void cadastrarNos() {

        addNo(1, -49.2672759, -25.4378885);
        addNo(2, -49.2679128, -25.4365983);
        addNo(6, -49.2602874, -25.4358262);
        addNo(7, -49.2599927, -25.4374212);
        addNo(8, -49.260268649, -25.4369571131);
        addNo(9, -49.2602754, -25.436389);
        addNo(10, -49.2600463, -25.4358271);
        addNo(11, -49.2585375, -25.4344617);
        addNo(29, -49.2577029, -25.432483);
        addNo(31, -49.265116, -25.4333509);
        addNo(32, -49.2637797, -25.4327882);
        addNo(38, -49.2623469, -25.4307067);
        addNo(39, -49.2579129, -25.4355521);
        addNo(40, -49.2584906, -25.4345535);
        addNo(41, -49.2605017, -25.4352225);
        addNo(42, -49.261076, -25.4339058);
        addNo(45, -49.2743964, -25.4370717);
        addNo(46, -49.2736492, -25.4387355);
        addNo(65, -49.2755048, -25.4375581);
        addNo(66, -49.2747178, -25.4391294);
        addNo(75, -49.2664928, -25.4360187);
        addNo(76, -49.2659427, -25.4372392);
        addNo(81, -49.2640277, -25.432203);
        addNo(82, -49.2679504, -25.432912);
        addNo(101, -49.2694831, -25.4334904);
        addNo(102, -49.2705344, -25.4338874);
        addNo(111, -49.2584167, -25.4327017);
        addNo(112, -49.2601804, -25.4334363);
        addNo(114, -49.2643114, -25.4350561);
        addNo(115, -49.2679638, -25.4364836);
        addNo(116, -49.2629735, -25.4346387);
        addNo(117, -49.2619159, -25.4341319);
        addNo(118, -49.2630158, -25.4345368);
        addNo(119, -49.2592007, -25.4331837);
        addNo(120, -49.2609237, -25.4338421);
        addNo(121, -49.2636482, -25.4364086);
        addNo(122, -49.2665372, -25.4359101);
        addNo(123, -49.2673488, -25.4342742);
        addNo(124, -49.265757, -25.437392);
        addNo(125, -49.2654254, -25.4372448);
        addNo(126, -49.2655487, -25.4382499);
        addNo(127, -49.2652744, -25.4388312);
        addNo(128, -49.2622964, -25.4360038);
        addNo(129, -49.261634, -25.4358339);
        addNo(130, -49.2628964, -25.4379337);
        addNo(131, -49.2619163, -25.4320279);
        addNo(133, -49.2603002, -25.4355435);
        addNo(134, -49.2604685, -25.4353169);
        addNo(138, -49.2657606, -25.4320666);
        addNo(139, -49.2621958, -25.4314257);
        addNo(144, -49.2600635, -25.4375105);
        addNo(155, -49.2750599, -25.435769);
        addNo(158, -49.2611063072, -25.4337951787);
        addNo(160, -49.260965, -25.4337351);
        addNo(167, -49.2645749, -25.4403145);
        addNo(173, -49.2605499, -25.4371038);
        addNo(176, -49.2612217, -25.4373732);
        addNo(178, -49.2636397, -25.4382143);
        addNo(179, -49.2634059, -25.4382794);
        addNo(196, -49.2733997, -25.4366747);
        addNo(198, -49.2613082, -25.4355312);
        addNo(199, -49.261758, -25.4319616);
        addNo(202, -49.2608856, -25.4381808);
        addNo(203, -49.2624475, -25.4387766);
        addNo(213, -49.2597071, -25.4355775);
        addNo(214, -49.2597867, -25.4358187);
        addNo(215, -49.2649975, -25.4388755);
        addNo(216, -49.264855, -25.4386766);
        addNo(217, -49.2667594, -25.4394049);
        addNo(218, -49.2665861, -25.439478);
        addNo(219, -49.2632357, -25.4382126);
        addNo(220, -49.2632352, -25.4380582);
        addNo(221, -49.2682956, -25.439975);
        addNo(222, -49.2681134, -25.4400269);
        addNo(223, -49.2683838, -25.4401326);
        addNo(228, -49.2718063, -25.4414616);
        addNo(229, -49.2717344, -25.441269);
        addNo(230, -49.2670071, -25.4396389);
        addNo(231, -49.26691, -25.4394663);
        addNo(232, -49.2658664, -25.4356567);
        addNo(233, -49.2660196, -25.4358351);
        addNo(238, -49.2636898, -25.4348129);
        addNo(239, -49.2638114, -25.4349847);
        addNo(240, -49.268362, -25.4367727);
        addNo(241, -49.2686513, -25.4367625);
        addNo(242, -49.2623239, -25.4344059);
        addNo(243, -49.2624909, -25.4343477);
        addNo(244, -49.2719593, -25.4382079);
        addNo(245, -49.2721268, -25.4381536);
        addNo(246, -49.2716717, -25.4379785);
        addNo(247, -49.2724716, -25.4362976);
        addNo(248, -49.2604371, -25.43365);
        addNo(249, -49.2606463, -25.4336159);
        addNo(250, -49.2712532, -25.4377924);
        addNo(251, -49.2714249, -25.4379986);
        addNo(252, -49.2615274, -25.4339687);
        addNo(253, -49.2616146, -25.4341261);
        addNo(256, -49.2730725, -25.4385134);
        addNo(257, -49.2731909, -25.4386808);
        addNo(258, -49.2696678, -25.437151);
        addNo(259, -49.2698081, -25.437346);
        addNo(262, -49.2720229, -25.4345131);
        addNo(281, -49.2698689, -25.4372396);
        addNo(283, -49.2756589, -25.4375282);
        addNo(284, -49.2603008, -25.4352576);
        addNo(285, -49.2623656, -25.4359235);
        addNo(286, -49.2611611, -25.4358492);
        addNo(287, -49.2603411, -25.4351518);
        addNo(292, -49.2595185, -25.4348422);
        addNo(293, -49.2592658, -25.4330586);
        addNo(294, -49.2607873, -25.4301263);
        addNo(297, -49.2591633, -25.4355686);
        addNo(298, -49.2570682, -25.4355571);
        addNo(299, -49.2570526, -25.4358037);
        addNo(301, -49.2601212, -25.4335484);
        addNo(302, -49.2618759, -25.4342352);
        addNo(303, -49.26101, -25.4316242);
        addNo(304, -49.260195, -25.4313045);
        addNo(305, -49.2582485, -25.4304648);
        addNo(306, -49.2624911, -25.4307676);
        addNo(308, -49.2643583, -25.4314784);
        addNo(310, -49.2690166, -25.4370183);
        addNo(311, -49.2690751, -25.4369214);
        addNo(312, -49.2705861, -25.4376494);
        addNo(313, -49.2725701, -25.4384337);
        addNo(314, -49.2726281, -25.438326);
        addNo(315, -49.2706366, -25.4375329);
        addNo(316, -49.2735871, -25.4388622);
        addNo(317, -49.2746677, -25.4392411);
        addNo(319, -49.2739889, -25.4405566);
        addNo(323, -49.2673269, -25.4377726);
        addNo(324, -49.2700898, -25.4388508);
        addNo(325, -49.2730542, -25.4399764);
        addNo(326, -49.2724029, -25.4415156);
        addNo(327, -49.2740869, -25.4403789);
        addNo(329, -49.2720195, -25.4396022);
        addNo(330, -49.2683234, -25.4383874);
        addNo(331, -49.263583, -25.4365285);
        addNo(332, -49.2694172, -25.4403836);
        addNo(333, -49.2684198, -25.4382006);
        addNo(338, -49.2719531, -25.4397937);
        addNo(339, -49.2700253, -25.4390268);
        addNo(341, -49.2652129, -25.438953);
        addNo(344, -49.2734213, -25.4419076);
        addNo(347, -49.2733404, -25.4420815);
        addNo(350, -49.26868, -25.441891);
        addNo(354, -49.2676208, -25.4398549);
        addNo(355, -49.2628239, -25.4380385);
        addNo(356, -49.2712883, -25.4412659);
        addNo(357, -49.2693542, -25.4405173);
        addNo(358, -49.2676762, -25.4397526);
        addNo(360, -49.2723313, -25.4416665);
        addNo(361, -49.2716444, -25.4429926);
        addNo(362, -49.2713561, -25.4411072);
        addNo(366, -49.2706608, -25.4425939);
        addNo(369, -49.271406, -25.4358663);
        addNo(371, -49.2628845, -25.4380635);
        addNo(372, -49.2630036, -25.4379674);
        addNo(376, -49.2611282, -25.4373401);
        addNo(382, -49.2698761, -25.4352862);
        addNo(383, -49.2688024, -25.4348478);
        addNo(386, -49.2592784, -25.4309084);
        addNo(387, -49.2594717, -25.4349341);
        addNo(447, -49.2669498, -25.4412502);
        addNo(472, -49.2612505, -25.4356383);
        addNo(475, -49.2707283, -25.4356092);
        addNo(476, -49.2699288, -25.437263);
        addNo(478, -49.2626886, -25.4323417);
        addNo(501, -49.2632296, -25.4310826);
        addNo(502, -49.2629528, -25.4317198);
        addNo(503, -49.2713742, -25.4342269);
        addNo(510, -49.2596813, -25.4332515);
        addNo(511, -49.2598724, -25.4334501);
        addNo(516, -49.2583343, -25.4328581);
        addNo(518, -49.2729921, -25.4401546);
        addNo(519, -49.261507, -25.4329471);
        addNo(520, -49.2614162, -25.4327438);
        addNo(527, -49.2642617, -25.4351647);
        addNo(559, -49.2716042, -25.4379469);
        addNo(560, -49.2715569, -25.4380645);
        addNo(571, -49.2659875, -25.4374864);
        addNo(572, -49.2660757, -25.4372888);
        addNo(573, -49.2671956, -25.4377265);
        addNo(574, -49.2671002, -25.4379201);
        addNo(674, -49.2648214944, -25.436821066);
        addNo(675, -49.2653341333, -25.4355960786);
        addNo(676, -49.2654205587, -25.4354760252);
        addNo(677, -49.2661957529, -25.4337912221);
        addNo(678, -49.2668059906, -25.4324647964);
        addNo(679, -49.265872816, -25.4381599348);
        addNo(680, -49.2616201234, -25.4304255503);

    }

    private void cadastrarMatrizPriori() {
        somaMatrixPriori = 0;

        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(89.5);
        a(116.35);
        a(0.0);
        a(62.65);
        a(44.75);
        a(44.75);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(44.75);
        a(0.0);
        a(179);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(17.9);
        a(152.15);
        a(0.0);
        a(8.95);
        a(0.0);
        a(134.25);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(35.45);
        a(35.45);
        a(0.0);
        a(14.18);
        a(14.18);
        a(56.72);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(14.18);
        a(0.0);
        a(354.5);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(106.35);
        a(0.0);
        a(35.45);
        a(0.0);
        a(42.54);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(10.55);
        a(25.32);
        a(0.0);
        a(21.1);
        a(16.88);
        a(63.3);
        a(0.0);
        a(5.275);
        a(0.0);
        a(0.0);
        a(5.275);
        a(0.0);
        a(10.55);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(21.1);
        a(10.55);
        a(0.0);
        a(0.0);
        a(0.0);
        a(21.1);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(96.3);
        a(250.38);
        a(0.0);
        a(134.82);
        a(96.3);
        a(385.2);
        a(0.0);
        a(577.8);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(48.15);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(173.34);
        a(48.15);
        a(0.0);
        a(19.26);
        a(0.0);
        a(96.3);
        a(32.6);
        a(0.0);
        a(65.2);
        a(0.0);
        a(0.0);
        a(65.2);
        a(0.0);
        a(0.0);
        a(234.72);
        a(0.0);
        a(65.2);
        a(110.84);
        a(312.96);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(65.2);
        a(0.0);
        a(65.2);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(130.4);
        a(65.2);
        a(0.0);
        a(26.08);
        a(0.0);
        a(65.2);
        a(4.725);
        a(0.0);
        a(4.725);
        a(0.0);
        a(0.0);
        a(34.02);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(4.725);
        a(28.35);
        a(0.0);
        a(4.725);
        a(0.0);
        a(0.0);
        a(9.45);
        a(0.0);
        a(14.175);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(46.305);
        a(32.13);
        a(0.0);
        a(5.67);
        a(0.0);
        a(0.0);
        a(183.75);
        a(0.0);
        a(196);
        a(0.0);
        a(49);
        a(122.5);
        a(0.0);
        a(122.5);
        a(0.0);
        a(0.0);
        a(0.0);
        a(61.25);
        a(245);
        a(0.0);
        a(61.25);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(122.5);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(1225);
        a(0.0);
        a(0.0);
        a(61.25);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(92.45);
        a(0.0);
        a(73.96);
        a(0.0);
        a(36.98);
        a(221.88);
        a(0.0);
        a(776.58);
        a(0.0);
        a(0.0);
        a(184.9);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(277.35);
        a(46.225);
        a(0.0);
        a(46.225);
        a(0.0);
        a(92.45);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(43.2);
        a(0.0);
        a(17.28);
        a(0.0);
        a(25.92);
        a(345.6);
        a(0.0);
        a(8.64);
        a(0.0);
        a(0.0);
        a(129.6);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(34.56);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(172.8);
        a(43.2);
        a(0.0);
        a(17.28);
        a(0.0);
        a(25.92);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(25.55);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(51.1);
        a(0.0);
        a(63.875);
        a(12.775);
        a(0.0);
        a(204.4);
        a(25.55);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(102.2);
        a(12.775);
        a(0.0);
        a(0.0);
        a(0.0);
        a(12.775);
        a(0.0);
        a(0.0);
        a(435.54);
        a(0.0);
        a(0.0);
        a(57.035);
        a(0.0);
        a(51.85);
        a(25.925);
        a(0.0);
        a(51.85);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(311.1);
        a(51.85);
        a(0.0);
        a(25.925);
        a(0.0);
        a(25.925);
        a(769.2);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(96.15);
        a(0.0);
        a(96.15);
        a(0.0);
        a(0.0);
        a(192.3);
        a(96.15);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(346.14);
        a(96.15);
        a(0.0);
        a(48.075);
        a(0.0);
        a(173.07);
        a(58.2);
        a(0.0);
        a(116.4);
        a(0.0);
        a(11.64);
        a(186.24);
        a(0.0);
        a(87.3);
        a(29.1);
        a(0.0);
        a(232.8);
        a(46.56);
        a(151.32);
        a(0.0);
        a(58.2);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(174.6);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(11.64);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(103.5);
        a(0.0);
        a(103.5);
        a(0.0);
        a(10.35);
        a(55.2);
        a(0.0);
        a(34.5);
        a(217.35);
        a(0.0);
        a(0.0);
        a(17.25);
        a(69);
        a(0.0);
        a(34.5);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(13.8);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(13.8);
        a(0.0);
        a(17.25);
        a(138.6);
        a(0.0);
        a(277.2);
        a(0.0);
        a(25.2);
        a(189);
        a(0.0);
        a(252);
        a(529.2);
        a(0.0);
        a(126);
        a(126);
        a(504);
        a(0.0);
        a(63);
        a(0.0);
        a(0.0);
        a(63);
        a(0.0);
        a(126);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(25.2);
        a(0.0);
        a(63);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(134.72);
        a(63.15);
        a(0.0);
        a(21.05);
        a(63.15);
        a(0.0);
        a(0.0);
        a(10.525);
        a(73.675);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(10.525);
        a(0.0);
        a(21.05);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(21.05);
        a(10.65);
        a(0.0);
        a(10.65);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(10.65);
        a(0.0);
        a(42.6);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(53.25);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(78.81);
        a(0.0);
        a(0.0);
        a(2.13);
        a(0.0);
        a(4.26);
        a(10);
        a(0.0);
        a(20);
        a(0.0);
        a(0.0);
        a(30);
        a(0.0);
        a(10);
        a(0.0);
        a(0.0);
        a(0.0);
        a(10);
        a(40);
        a(0.0);
        a(10);
        a(0.0);
        a(0.0);
        a(20);
        a(0.0);
        a(30);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(20);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        a(0.0);
        System.out.println("OK: Matriz priori tem " + matrizPriori.size() + " elementos. NumPontosOD = " + (int) Math.sqrt(this.matrizPriori.size()));
        this.numPontosOD = (int) Math.sqrt(this.matrizPriori.size());
    }

    /**
     * @return the fromNoC
     */
    public ArrayList<String> getFromNoC() {
        return fromNoC;
    }

    /**
     * @return the toNoC
     */
    public ArrayList<String> getToNoC() {
        return toNoC;
    }

    /**
     * @return the capacidade
     */
    public ArrayList<Integer> getCapacidade() {
        return capacidade;
    }

    /**
     * @return the capacidadeSemaforo
     */
    public ArrayList<Integer> getCapacidadeSemaforo() {
        return capacidadeSemaforo;
    }

    /**
     * @return the contLink
     */
    public ArrayList<Integer> getContLink() {
        return contLink;
    }

    public int getContLink(int a) {
        return contLink.get(a);
    }

    public int getContLink(String fromN, String toN) {
        int from, to;
        from = this.getNodeIndex(fromN);
        to = this.getNodeIndex(toN);
        return getContLink(from, to);
    }

    public int getContLink(int fromN, int toN) {
        for (int a = 0; a < this.contArestas; a++) {
            if (toNo.get(a) == toN && fromNo.get(a) == fromN) {
                return getContLink(a);
            }
        }
        return -1;
    }

    public int getContLinksMaiorZero() {
        int cont = 0;
        for (int c = 0; c < contLink.size(); c++) {
            if (contLink.get(c) > 0) {
                cont++;
            }
        }
        return cont;
    }

    /**
     * @return the velocidade
     */
    public ArrayList<Integer> getVelocidade() {
        return velocidade;
    }

    /**
     * @return the geh
     */
    public double[] getGeh() {
        return geh;
    }

    /**
     * @return the latNo
     */
    public ArrayList<Double> getLatNo() {
        return latNo;
    }

    /**
     * @return the lonNo
     */
    public ArrayList<Double> getLonNo() {
        return lonNo;
    }

    /**
     * @return the codNo
     */
    public ArrayList<String> getCodNo() {
        return codNo;
    }

    /**
     * @return the fromNo
     */
    public ArrayList<Integer> getFromNo() {
        return fromNo;
    }

    /**
     * @return the toNo
     */
    public ArrayList<Integer> getToNo() {
        return toNo;
    }

    /**
     * @return the tipoLink
     */
    public ArrayList<Integer> getTipoLink() {
        return tipoLink;
    }

    /**
     * @return the matrizPriori
     */
    public ArrayList<Double> getMatrizPriori() {
        return matrizPriori;
    }

    /**
     * @param matrizPriori the matrizPriori to set
     */
    public void setMatrizPriori(ArrayList<Double> matrizPriori) {
        this.matrizPriori = matrizPriori;
    }

    /**
     * @return the numPontosOD
     */
    public int getNumPontosOD() {
        return numPontosOD;
    }

    /**
     * @return the linksComContadores
     */
    public int getLinksComContadores() {
        return linksComContadores;
    }

    /**
     * @param fluxosParaBPR the fluxosParaBPR to set
     */
    public void setFluxosParaBPR(double[] fluxosParaBPR) {
        this.fluxosParaBPR = fluxosParaBPR;
    }

    public int getArestaIndex(String noF, String noT) {
        for (int x = 0; x < fromNoC.size(); x++) {
            if (fromNoC.get(x).equals(noF)) {
                if (toNoC.get(x).equals(noT)) {
                    return x;
                }
            }
        }

        return -1;
    }

    public void gerarAssignmentMatrix_FromVissum(Mapping map) {
        System.out.println("PROC: Fazendo importação de assignment matrix (" + numPontosOD + " pontos OD)... " + horaAtual());
        setMape(map);
        System.out.println("INFO: Mapping possui " + getMape().getContArestas() + " arestas (like '" + getMape().getFromNod().get(0) + "'to'" + getMape().getToNod().get(0) + "') "
                + "e " + getMape().getContNodes() + " nodes (like '" + getMape().getNode_id()[0] + "').");

        ArrayList<Integer> aresta;
        ArrayList<Double> prob;

        doParOD = new ArrayList<>();
        probODaresta = new ArrayList<>();
        daAresta = new ArrayList<>();

//importar da planilha
        visumRotas();
        visumFluxoRotas();

//normalizar vehRota[][][]
        double sum;
        for (int o = 0; o < numPontosOD; o++) {
            for (int d = 0; d < numPontosOD; d++) {
                sum = 0.0;
                for (int c = 0; c < contRotas[o][d]; c++) {
                    sum = sum + vehRota[o][d][c];
                }
                for (int c = 0; c < contRotas[o][d]; c++) {
                    //System.out.println("vehRota["+o+"]["+d+"]["+c+"] = " + (vehRota[o][d][c] / sum) + " =  "+vehRota[o][d][c]+ "  / "+sum);
                    vehRota[o][d][c] = (vehRota[o][d][c] / sum);
                }
            }
        }

        int ar;
//join de rotas para mesmo par OD
        for (int o = 0; o < numPontosOD; o++) {
            for (int d = 0; d < numPontosOD; d++) {

                aresta = new ArrayList<>();
                prob = new ArrayList<>();

                for (int c = 0; c < contRotas[o][d]; c++) {
                    for (int L = 0; L < contLinkRota[o][d][c]; L++) {

                        //identificar aresta
                        ar = getMape().getArestaIndex(noFromRota[o][d][c][L], noToRota[o][d][c][L]);
                        //ar = this.getArestaIndex(noFromRota[o][d][c][L], noToRota[o][d][c][L]);

                        if (aresta.contains(ar)) { // se já está inclusa,
                            ar = aresta.indexOf(ar);
                            prob.set(ar, prob.get(ar) + vehRota[o][d][c]); //atualiza probabilidade somente
                        } else if (ar != -1) { //se não esta inclusa, e foi identificada
                            aresta.add(ar);
                            prob.add(vehRota[o][d][c]);
                            //System.out.println("ADD OD "+o+" "+d+"; r"+c+"; lnk "+L);
                        } else { //se for aresta não identificada
                            System.out.print("Aresta nao encontrada '" + noFromRota[o][d][c][L] + "' to '" + noToRota[o][d][c][L] + "'");
                            System.out.println(" o=" + o + " d=" + d + " c=" + c + " L=" + L);
                        }

                    }
                }

//acrescentar nos arrays doParOD, probODaresta, daAresta
                for (int a = 0; a < aresta.size(); a++) {
                    doParOD.add(o * numPontosOD + d);
                    probODaresta.add(prob.get(a));
                    daAresta.add(aresta.get(a));
                }

            }
        }

        System.out.println("OK:" + doParOD.size() + " prob aresta/parOD. ");
        testarMODSemente();
        
        demonstraRotas();

    }

    private void addTrechoRota(int o, int d, int r, int link, String noOrg, String noDest) {
        linkRota[o - 1][d - 1][r - 1][contLinkRota[o - 1][d - 1][r - 1]] = link;

        noFromRota[o - 1][d - 1][r - 1][contLinkRota[o - 1][d - 1][r - 1]] = "n" + noOrg;
        noToRota[o - 1][d - 1][r - 1][contLinkRota[o - 1][d - 1][r - 1]] = "n" + noDest.replace("\n", "");

        contLinkRota[o - 1][d - 1][r - 1]++;
        
        
        
    }

    private void addPercRota(int o, int d, int r, String veh) {
        double q = Double.valueOf(veh.replace(",", "."));
        //System.out.println("vehRota["+(o-1)+"]["+(d-1)+"]["+(r-1)+"] = "+q);
        vehRota[o - 1][d - 1][r - 1] = q;
        if (contRotas[o - 1][d - 1] < r) {
            contRotas[o - 1][d - 1] = r;
        }
    }

    private void addTrechoRota(String o, String d, String r, String link, String noOrg, String noDest) {
        addTrechoRota(Integer.valueOf(o), Integer.valueOf(d), Integer.valueOf(r), Integer.valueOf(link), noOrg, noDest);
        // System.out.println("o="+o+" d="+d+" r="+r+" lnk="+link+" nOrg="+noOrg+" noDest="+noDest);
    }

    Double[][][] vehRota; //add Perc Rota
    int[][] contRotas; // add Perc Rota
    int[][][][] linkRota; // add Trecho Rota
    String[][][][] noFromRota;  // add Trecho Rota
    String[][][][] noToRota; // add Trecho Rota
    int[][][] contLinkRota; // add Trecho Rota

    private void visumRotas() {

        int nRo = 25;
        int tRo = 40;

        contRotas = new int[numPontosOD][numPontosOD];
        vehRota = new Double[numPontosOD][numPontosOD][nRo];
        linkRota = new int[numPontosOD][numPontosOD][nRo][tRo];
        noFromRota = new String[numPontosOD][numPontosOD][nRo][tRo];
        noToRota = new String[numPontosOD][numPontosOD][nRo][tRo];
        contLinkRota = new int[numPontosOD][numPontosOD][nRo];

        for (int x = 0; x < numPontosOD; x++) //começar com vetores limpos
        {
            for (int y = 0; y < numPontosOD; y++) {

                contRotas[x][y] = 0;
                for (int z = 0; z < nRo; z++) {
                    vehRota[x][y][z] = 0.0;
                    contLinkRota[x][y][z] = 0;
                    for (int w = 0; w < tRo; w++) {
                        linkRota[x][y][z][w] = 0;
                        noFromRota[x][y][z][w] = "-";
                        noToRota[x][y][z][w] = "-";
                    }
                }

            }
        }

        ArrayList<String> dados = this.lerTxt("visumRotas.txt"); //ler arquivo com inf das rotas

        for (int d = 0; d < dados.size(); d++) {
            String[] div = dados.get(d).split(",");
            //System.out.println(dados.get(d)); 
            addTrechoRota(div[0], div[1], div[2], div[3], div[4], div[5]);
        }

    
     
        
    }

    
    public void demonstraRotas(){
     criaDiretorio("demonstra_rotas");   
        
      for (int x = 0; x < numPontosOD; x++){
            for (int y = 0; y < numPontosOD; y++) {
     
                String pontos = "";
                //linkRota[o - 1][d - 1][r - 1][contLinkRota[o - 1][d - 1][r - 1]] = link;
              
                /*doParOD.add(o * numPontosOD + d);
                    probODaresta.add(prob.get(a));
                    daAresta.add(aresta.get(a));*/
                
                
                for(int p=0;p<doParOD.size();p++)
                    if(doParOD.get(p)==(x*numPontosOD + y)){
                        pontos = pontos + this.lonNo.get(this.fromNo.get(daAresta.get(p)))+ ", " + latNo.get(fromNo.get(daAresta.get(p))) +  "\n";
                        pontos = pontos + this.lonNo.get(toNo.get(daAresta.get(p)))+ ", " + latNo.get(toNo.get(daAresta.get(p))) +  "\n";
                    }
                
                
                /*for(int r=0;r<contRotas[x][y];r++){
                    for(int i=0;i<contLinkRota[x][y][r];i++)
                        pontos = pontos + "\n";                
                }*/
                
                if(pontos.length()>19)
                    salvarTxt("demonstra_rotas\\"+x+"to"+y+".txt",pontos);
                
            }
      }
    
    }
    
    
    public static boolean salvarTxt(String name, String conteudo) {

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(name);
            fileWriter.append(conteudo);
        } catch (IOException e) {
            System.out.println("ERROR: FileWriter de '" + name + ".");
            //e.printStackTrace();
            return false;
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
                return true;
            } catch (IOException e) {
                System.out.println("ERROR: While flushing/closing fileWriter de '" + name + "'.");
                // e.printStackTrace();
                return false;
            }
        }
    }
    
    
       public void criaDiretorio(String novoDiretorio) {

        try {
            if (!new File(novoDiretorio).exists()) { // Verifica se o diretório existe.   
                (new File(novoDiretorio)).mkdir();   // Cria o diretório   
            }
        } catch (Exception ex) {
            System.out.println("ERROR: Falha ao criar diretório " + novoDiretorio);
        }
    }
    
    
    private void visumFluxoRotas() {

        addPercRota(2, 8, 1, "38,877");
        addPercRota(2, 8, 2, "66,732");
        addPercRota(2, 9, 1, "38,76");
        addPercRota(2, 9, 2, "66,532");
        addPercRota(2, 11, 1, "20,764");
        addPercRota(2, 11, 2, "35,642");
        addPercRota(2, 12, 1, "19,421");
        addPercRota(2, 12, 2, "33,337");
        addPercRota(2, 13, 1, "12,298");
        addPercRota(2, 13, 2, "21,109");
        addPercRota(2, 18, 1, "0,02");
        addPercRota(2, 18, 2, "56,118");
        addPercRota(2, 20, 1, "60,471");
        addPercRota(2, 20, 2, "173,541");
        addPercRota(2, 25, 1, "24,611");
        addPercRota(2, 26, 1, "80,078");
        addPercRota(2, 28, 1, "24,734");
        addPercRota(2, 30, 1, "49,739");
        addPercRota(2, 30, 2, "85,377");
        addPercRota(4, 8, 1, "4,829");
        addPercRota(4, 8, 2, "35,371");
        addPercRota(4, 9, 1, "3,735");
        addPercRota(4, 9, 2, "27,361");
        addPercRota(4, 11, 1, "1,474");
        addPercRota(4, 11, 2, "10,796");
        addPercRota(4, 12, 1, "1,93");
        addPercRota(4, 12, 2, "14,136");
        addPercRota(4, 13, 1, "4,975");
        addPercRota(4, 13, 2, "36,443");
        addPercRota(4, 18, 1, "14,855");
        addPercRota(4, 20, 1, "402,488");
        addPercRota(4, 26, 1, "103,769");
        addPercRota(4, 28, 1, "49,874");
        addPercRota(4, 30, 1, "5,098");
        addPercRota(4, 30, 2, "37,342");
        addPercRota(5, 8, 1, "1,301");
        addPercRota(5, 8, 2, "10,573");
        addPercRota(5, 9, 1, "2,287");
        addPercRota(5, 9, 2, "18,587");
        addPercRota(5, 11, 1, "1,895");
        addPercRota(5, 11, 2, "15,401");
        addPercRota(5, 12, 1, "2,009");
        addPercRota(5, 12, 2, "16,325");
        addPercRota(5, 13, 1, "4,714");
        addPercRota(5, 13, 2, "38,308");
        addPercRota(5, 15, 1, "0,678");
        addPercRota(5, 15, 2, "5,51");
        addPercRota(5, 18, 1, "0,435");
        addPercRota(5, 18, 2, "3,533");
        addPercRota(5, 20, 1, "0,229");
        addPercRota(5, 20, 2, "1,861");
        addPercRota(5, 20, 3, "0,383");
        addPercRota(5, 20, 4, "3,117");
        addPercRota(5, 25, 1, "3,145");
        addPercRota(5, 25, 2, "25,557");
        addPercRota(5, 26, 1, "4,775");
        addPercRota(5, 26, 2, "38,81");
        addPercRota(5, 26, 3, "0,142");
        addPercRota(5, 26, 4, "1,154");
        addPercRota(5, 30, 1, "2,134");
        addPercRota(5, 30, 2, "17,344");
        addPercRota(7, 8, 1, "85,488");
        addPercRota(7, 9, 1, "172,209");
        addPercRota(7, 11, 1, "91,727");
        addPercRota(7, 12, 1, "85,413");
        addPercRota(7, 13, 1, "216,896");
        addPercRota(7, 15, 1, "590,122");
        addPercRota(7, 20, 1, "44,417");
        addPercRota(7, 20, 2, "0,195");
        addPercRota(7, 25, 1, "115,547");
        addPercRota(7, 25, 2, "0,573");
        addPercRota(7, 25, 3, "8,802");
        addPercRota(7, 25, 4, "16,371");
        addPercRota(7, 25, 5, "32,65");
        addPercRota(7, 25, 6, "289,188");
        addPercRota(7, 26, 1, "160,843");
        addPercRota(7, 26, 2, "0,001");
        addPercRota(7, 26, 3, "0,018");
        addPercRota(7, 26, 4, "0,034");
        addPercRota(7, 26, 5, "0,068");
        addPercRota(7, 26, 6, "0,604");
        addPercRota(7, 28, 1, "2,159");
        addPercRota(7, 28, 2, "4,306");
        addPercRota(7, 28, 3, "38,137");
        addPercRota(7, 30, 1, "73,46");
        addPercRota(8, 1, 1, "4,707");
        addPercRota(8, 1, 2, "20,168");
        addPercRota(8, 1, 3, "3,255");
        addPercRota(8, 1, 4, "23,368");
        addPercRota(8, 3, 1, "74,319");
        addPercRota(8, 6, 1, "121,405");
        addPercRota(8, 9, 1, "189,84");
        addPercRota(8, 11, 1, "51,794");
        addPercRota(8, 12, 1, "115,819");
        addPercRota(8, 13, 1, "206,795");
        addPercRota(8, 18, 1, "19,02");
        addPercRota(8, 18, 2, "3,07");
        addPercRota(8, 18, 3, "9,466");
        addPercRota(8, 20, 1, "35,216");
        addPercRota(8, 20, 2, "2,349");
        addPercRota(8, 20, 3, "10,063");
        addPercRota(8, 20, 4, "1,624");
        addPercRota(8, 25, 1, "22,987");
        addPercRota(8, 25, 2, "98,482");
        addPercRota(8, 25, 3, "15,895");
        addPercRota(8, 26, 1, "40,872");
        addPercRota(8, 26, 2, "14,784");
        addPercRota(8, 26, 3, "63,338");
        addPercRota(8, 26, 4, "10,223");
        addPercRota(8, 26, 5, "73,386");
        addPercRota(8, 28, 1, "35,346");
        addPercRota(8, 30, 1, "58,331");
        addPercRota(9, 1, 1, "0,511");
        addPercRota(9, 1, 2, "0,335");
        addPercRota(9, 1, 3, "0,215");
        addPercRota(9, 1, 4, "0,091");
        addPercRota(9, 1, 5, "11,232");
        addPercRota(9, 3, 1, "0,204");
        addPercRota(9, 3, 2, "0,131");
        addPercRota(9, 3, 3, "0,056");
        addPercRota(9, 3, 4, "6,837");
        addPercRota(9, 6, 1, "0,626");
        addPercRota(9, 6, 2, "77,078");
        addPercRota(9, 12, 1, "9,377");
        addPercRota(9, 13, 1, "98,633");
        addPercRota(9, 15, 1, "11,806");
        addPercRota(9, 18, 1, "15,194");
        addPercRota(9, 20, 1, "15,113");
        addPercRota(9, 25, 1, "133,556");
        addPercRota(9, 26, 1, "3,571");
        addPercRota(9, 26, 2, "2,337");
        addPercRota(9, 26, 3, "1,504");
        addPercRota(9, 26, 4, "0,637");
        addPercRota(9, 26, 5, "78,427");
        addPercRota(9, 28, 1, "0,186");
        addPercRota(9, 28, 2, "0,079");
        addPercRota(9, 28, 3, "9,715");
        addPercRota(10, 1, 1, "0,404");
        addPercRota(10, 1, 2, "3,128");
        addPercRota(10, 1, 3, "0,172");
        addPercRota(10, 1, 4, "1,449");
        addPercRota(10, 1, 5, "11,206");
        addPercRota(10, 1, 6, "0,618");
        addPercRota(10, 1, 7, "1,457");
        addPercRota(10, 1, 8, "11,274");
        addPercRota(10, 1, 9, "0,622");
        addPercRota(10, 1, 10, "0,503");
        addPercRota(10, 1, 11, "3,892");
        addPercRota(10, 1, 12, "0,215");
        addPercRota(10, 1, 13, "144,774");
        addPercRota(10, 3, 1, "0,935");
        addPercRota(10, 3, 2, "7,231");
        addPercRota(10, 3, 3, "0,399");
        addPercRota(10, 3, 4, "0,94");
        addPercRota(10, 3, 5, "7,274");
        addPercRota(10, 3, 6, "0,401");
        addPercRota(10, 3, 7, "0,325");
        addPercRota(10, 3, 8, "2,511");
        addPercRota(10, 3, 9, "0,138");
        addPercRota(10, 3, 10, "93,413");
        addPercRota(10, 5, 1, "0,252");
        addPercRota(10, 5, 2, "1,948");
        addPercRota(10, 5, 3, "0,107");
        addPercRota(10, 5, 4, "0,253");
        addPercRota(10, 5, 5, "1,959");
        addPercRota(10, 5, 6, "0,108");
        addPercRota(10, 5, 7, "0,087");
        addPercRota(10, 5, 8, "0,676");
        addPercRota(10, 5, 9, "0,037");
        addPercRota(10, 5, 10, "25,162");
        addPercRota(10, 6, 1, "0,312");
        addPercRota(10, 6, 2, "2,417");
        addPercRota(10, 6, 3, "0,133");
        addPercRota(10, 6, 4, "89,911");
        addPercRota(10, 8, 1, "53,025");
        addPercRota(10, 12, 1, "57,103");
        addPercRota(10, 13, 1, "69,697");
        addPercRota(10, 13, 2, "539,151");
        addPercRota(10, 13, 3, "88,969");
        addPercRota(10, 15, 1, "8,105");
        addPercRota(10, 15, 2, "62,695");
        addPercRota(10, 15, 3, "3,457");
        addPercRota(10, 20, 1, "6,7");
        addPercRota(10, 20, 2, "51,832");
        addPercRota(10, 20, 3, "2,858");
        addPercRota(10, 25, 1, "52,9");
        addPercRota(10, 25, 2, "409,212");
        addPercRota(10, 25, 3, "22,567");
        addPercRota(10, 25, 4, "1,759");
        addPercRota(10, 25, 5, "13,606");
        addPercRota(10, 25, 6, "0,75");
        addPercRota(10, 25, 7, "6,302");
        addPercRota(10, 25, 8, "48,75");
        addPercRota(10, 25, 9, "2,688");
        addPercRota(10, 25, 10, "6,34");
        addPercRota(10, 25, 11, "49,045");
        addPercRota(10, 25, 12, "2,705");
        addPercRota(10, 25, 13, "2,189");
        addPercRota(10, 25, 14, "16,933");
        addPercRota(10, 25, 15, "0,934");
        addPercRota(10, 25, 16, "629,811");
        addPercRota(10, 28, 1, "0,328");
        addPercRota(10, 28, 2, "2,536");
        addPercRota(10, 28, 3, "0,14");
        addPercRota(10, 28, 4, "0,113");
        addPercRota(10, 28, 5, "0,876");
        addPercRota(10, 28, 6, "0,048");
        addPercRota(10, 28, 7, "32,572");
        addPercRota(14, 1, 1, "38,772");
        addPercRota(14, 1, 2, "35,56");
        addPercRota(14, 3, 1, "8,821");
        addPercRota(14, 3, 2, "10,047");
        addPercRota(14, 3, 3, "2,843");
        addPercRota(14, 3, 4, "48,775");
        addPercRota(14, 5, 1, "4,752");
        addPercRota(14, 5, 2, "5,413");
        addPercRota(14, 5, 3, "1,531");
        addPercRota(14, 5, 4, "26,276");
        addPercRota(14, 6, 1, "182,86");
        addPercRota(14, 6, 2, "5,509");
        addPercRota(14, 6, 3, "94,53");
        addPercRota(14, 8, 1, "830,335");
        addPercRota(14, 11, 1, "228,846");
        addPercRota(14, 25, 1, "193,445");
        addPercRota(14, 26, 1, "13,514");
        addPercRota(14, 26, 2, "12,395");
        addPercRota(14, 28, 1, "8,471");
        addPercRota(14, 28, 2, "2,397");
        addPercRota(14, 28, 3, "41,124");
        addPercRota(14, 30, 1, "84,482");
        addPercRota(16, 1, 1, "1,554");
        addPercRota(16, 1, 2, "0,713");
        addPercRota(16, 1, 3, "8,959");
        addPercRota(16, 3, 1, "0,592");
        addPercRota(16, 3, 2, "7,437");
        addPercRota(16, 5, 1, "0,976");
        addPercRota(16, 5, 2, "12,254");
        addPercRota(16, 6, 1, "414,295");
        addPercRota(16, 8, 1, "8,695");
        addPercRota(16, 11, 1, "14,42");
        addPercRota(16, 11, 2, "139,753");
        addPercRota(16, 20, 1, "5,415");
        addPercRota(16, 25, 1, "71,984");
        addPercRota(16, 26, 1, "1,083");
        addPercRota(16, 26, 2, "0,497");
        addPercRota(16, 26, 3, "6,246");
        addPercRota(16, 28, 1, "8,533");
        addPercRota(16, 30, 1, "21,711");
        addPercRota(21, 1, 1, "13,652");
        addPercRota(21, 6, 1, "58,162");
        addPercRota(21, 8, 1, "58,892");
        addPercRota(21, 9, 1, "9,851");
        addPercRota(21, 11, 1, "0,122");
        addPercRota(21, 11, 2, "230,985");
        addPercRota(21, 12, 1, "0,02");
        addPercRota(21, 12, 2, "38,549");
        addPercRota(21, 25, 1, "103,353");
        addPercRota(21, 26, 1, "4,759");
        addPercRota(21, 30, 1, "10,339");
        addPercRota(22, 3, 1, "627,473");
        addPercRota(22, 6, 1, "63,968");
        addPercRota(22, 8, 1, "5,356");
        addPercRota(22, 8, 2, "1,223");
        addPercRota(22, 8, 3, "40,021");
        addPercRota(22, 9, 1, "18,371");
        addPercRota(22, 11, 1, "3,923");
        addPercRota(22, 11, 2, "0,896");
        addPercRota(22, 11, 3, "29,316");
        addPercRota(22, 11, 4, "5,084");
        addPercRota(22, 25, 1, "252,472");
        addPercRota(22, 26, 1, "22,531");
        addPercRota(22, 28, 1, "18,446");
        addPercRota(22, 30, 1, "19,282");
        addPercRota(23, 1, 1, "829,957");
        addPercRota(23, 6, 1, "101,788");
        addPercRota(23, 8, 1, "1,657");
        addPercRota(23, 8, 2, "3,613");
        addPercRota(23, 8, 3, "74,432");
        addPercRota(23, 11, 1, "1,695");
        addPercRota(23, 11, 2, "3,696");
        addPercRota(23, 11, 3, "76,129");
        addPercRota(23, 11, 4, "79,615");
        addPercRota(23, 12, 1, "1,11");
        addPercRota(23, 12, 2, "2,42");
        addPercRota(23, 12, 3, "49,844");
        addPercRota(23, 12, 4, "52,126");
        addPercRota(23, 25, 1, "334,154");
        addPercRota(23, 26, 1, "72,228");
        addPercRota(23, 28, 1, "33,954");
        addPercRota(23, 30, 1, "135,271");
        addPercRota(24, 1, 1, "68,816");
        addPercRota(24, 3, 1, "100,902");
        addPercRota(24, 3, 2, "58,804");
        addPercRota(24, 3, 3, "2,457");
        addPercRota(24, 5, 1, "10,126");
        addPercRota(24, 5, 2, "5,901");
        addPercRota(24, 5, 3, "2,047");
        addPercRota(24, 6, 1, "119,731");
        addPercRota(24, 6, 2, "69,777");
        addPercRota(24, 6, 3, "1,472");
        addPercRota(24, 6, 4, "0,858");
        addPercRota(24, 6, 5, "0,297");
        addPercRota(24, 8, 1, "0,016");
        addPercRota(24, 8, 2, "0,009");
        addPercRota(24, 8, 3, "0,003");
        addPercRota(24, 8, 4, "57,477");
        addPercRota(24, 8, 5, "16,491");
        addPercRota(24, 8, 6, "9,611");
        addPercRota(24, 9, 1, "12,576");
        addPercRota(24, 9, 2, "7,329");
        addPercRota(24, 11, 1, "0,007");
        addPercRota(24, 11, 2, "0,004");
        addPercRota(24, 11, 3, "0,001");
        addPercRota(24, 11, 4, "24,49");
        addPercRota(24, 11, 5, "7,027");
        addPercRota(24, 11, 6, "4,095");
        addPercRota(24, 11, 7, "123,73");
        addPercRota(24, 11, 8, "72,107");
        addPercRota(24, 12, 1, "0,002");
        addPercRota(24, 12, 2, "0,001");
        addPercRota(24, 12, 3, "0");
        addPercRota(24, 12, 4, "6,469");
        addPercRota(24, 12, 5, "1,856");
        addPercRota(24, 12, 6, "1,082");
        addPercRota(24, 12, 7, "32,682");
        addPercRota(24, 12, 8, "19,046");
        addPercRota(24, 13, 1, "0,017");
        addPercRota(24, 13, 2, "0,01");
        addPercRota(24, 13, 3, "0,003");
        addPercRota(24, 13, 4, "60,049");
        addPercRota(24, 13, 5, "17,229");
        addPercRota(24, 13, 6, "10,041");
        addPercRota(24, 15, 1, "28,047");
        addPercRota(24, 15, 2, "16,345");
        addPercRota(24, 20, 1, "90,074");
        addPercRota(24, 20, 2, "52,493");
        addPercRota(24, 28, 1, "5,225");
        addPercRota(24, 28, 2, "3,045");
        addPercRota(26, 1, 1, "10,011");
        addPercRota(26, 3, 1, "11,706");
        addPercRota(26, 5, 1, "1,273");
        addPercRota(26, 6, 1, "2,277");
        addPercRota(26, 8, 1, "8,307");
        addPercRota(26, 9, 1, "290,248");
        addPercRota(26, 9, 2, "623,247");
        addPercRota(26, 12, 1, "2,005");
        addPercRota(26, 13, 1, "5,154");
        addPercRota(26, 15, 1, "4,225");
        addPercRota(26, 20, 1, "1,769");
        addPercRota(26, 28, 1, "3,985");
        addPercRota(26, 30, 1, "6,167");
        addPercRota(27, 1, 1, "165,291");
        addPercRota(27, 3, 1, "385,165");
        addPercRota(27, 5, 1, "0,483");
        addPercRota(27, 5, 2, "36,971");
        addPercRota(27, 6, 1, "66,982");
        addPercRota(27, 6, 2, "2,942");
        addPercRota(27, 6, 3, "225,179");
        addPercRota(27, 8, 1, "117,657");
        addPercRota(27, 8, 2, "27,896");
        addPercRota(27, 8, 3, "1,225");
        addPercRota(27, 8, 4, "93,78");
        addPercRota(27, 9, 1, "191,052");
        addPercRota(27, 9, 2, "45,298");
        addPercRota(27, 9, 3, "1,99");
        addPercRota(27, 9, 4, "152,281");
        addPercRota(27, 11, 1, "44,887");
        addPercRota(27, 11, 2, "10,642");
        addPercRota(27, 11, 3, "0,467");
        addPercRota(27, 11, 4, "35,778");
        addPercRota(27, 12, 1, "58,777");
        addPercRota(27, 12, 2, "13,936");
        addPercRota(27, 12, 3, "0,612");
        addPercRota(27, 12, 4, "46,849");
        addPercRota(27, 13, 1, "170,75");
        addPercRota(27, 13, 2, "40,484");
        addPercRota(27, 13, 3, "1,778");
        addPercRota(27, 13, 4, "136,099");
        addPercRota(27, 15, 1, "17,899");
        addPercRota(27, 15, 2, "63,229");
        addPercRota(27, 18, 1, "17,831");
        addPercRota(27, 18, 2, "62,988");
        addPercRota(27, 20, 1, "35,729");
        addPercRota(27, 20, 2, "136,977");
        addPercRota(27, 28, 1, "66,015");
        addPercRota(27, 30, 1, "25,276");
        addPercRota(27, 30, 2, "5,993");
        addPercRota(27, 30, 3, "0,263");
        addPercRota(27, 30, 4, "20,146");
        addPercRota(28, 5, 1, "92,868");
        addPercRota(28, 6, 1, "27,396");
        addPercRota(28, 6, 2, "35,11");
        addPercRota(28, 8, 1, "35,731");
        addPercRota(28, 8, 2, "0,456");
        addPercRota(28, 8, 3, "0,585");
        addPercRota(28, 9, 1, "82,918");
        addPercRota(28, 9, 2, "1,059");
        addPercRota(28, 9, 3, "1,357");
        addPercRota(28, 12, 1, "18,7");
        addPercRota(28, 12, 2, "0,239");
        addPercRota(28, 12, 3, "0,306");
        addPercRota(28, 13, 1, "85,749");
        addPercRota(28, 13, 2, "1,095");
        addPercRota(28, 13, 3, "1,403");
        addPercRota(28, 18, 1, "0,563");
        addPercRota(28, 18, 2, "0,007");
        addPercRota(28, 18, 3, "0,009");
        addPercRota(28, 18, 4, "13,197");
        addPercRota(28, 18, 5, "0,169");
        addPercRota(28, 18, 6, "0,216");
        addPercRota(28, 20, 1, "0,256");
        addPercRota(28, 20, 2, "0,003");
        addPercRota(28, 20, 3, "0,004");
        addPercRota(28, 20, 4, "16,863");
        addPercRota(28, 20, 5, "0,215");
        addPercRota(28, 20, 6, "0,276");
        addPercRota(28, 30, 1, "30,704");
        addPercRota(28, 30, 2, "0,392");
        addPercRota(28, 30, 3, "0,502");
        addPercRota(29, 1, 1, "17,093");
        addPercRota(29, 1, 2, "0,927");
        addPercRota(29, 1, 3, "0,36");
        addPercRota(29, 1, 4, "0,687");
        addPercRota(29, 3, 1, "22,593");
        addPercRota(29, 3, 2, "8,784");
        addPercRota(29, 3, 3, "16,739");
        addPercRota(29, 9, 1, "10,528");
        addPercRota(29, 11, 1, "61,587");
        addPercRota(29, 20, 1, "59,835");
        addPercRota(29, 25, 1, "239,886");
        addPercRota(29, 28, 1, "2,941");
        addPercRota(29, 28, 2, "5,605");
        addPercRota(29, 30, 1, "4,253");
        addPercRota(30, 1, 1, "0,043");
        addPercRota(30, 1, 2, "0,027");
        addPercRota(30, 1, 3, "0,785");
        addPercRota(30, 1, 4, "9,688");
        addPercRota(30, 1, 5, "6,175");
        addPercRota(30, 3, 1, "0,067");
        addPercRota(30, 3, 2, "0,043");
        addPercRota(30, 3, 3, "1,243");
        addPercRota(30, 3, 4, "15,344");
        addPercRota(30, 3, 5, "9,78");
        addPercRota(30, 6, 1, "0,162");
        addPercRota(30, 6, 2, "0,103");
        addPercRota(30, 6, 3, "2,992");
        addPercRota(30, 6, 4, "36,944");
        addPercRota(30, 6, 5, "23,548");
        addPercRota(30, 8, 1, "7,129");
        addPercRota(30, 8, 2, "4,544");
        addPercRota(30, 12, 1, "18,755");
        addPercRota(30, 13, 1, "31,311");
        addPercRota(30, 15, 1, "12,448");
        addPercRota(30, 18, 1, "0,609");
        addPercRota(30, 18, 2, "0,388");
        addPercRota(30, 18, 3, "11,233");
        addPercRota(30, 18, 4, "10,052");
        addPercRota(30, 20, 1, "5,867");
        addPercRota(30, 20, 2, "0,772");
        addPercRota(30, 20, 3, "0,492");
        addPercRota(30, 20, 4, "14,24");
        addPercRota(30, 25, 1, "1,908");
        addPercRota(30, 25, 2, "1,216");
        addPercRota(30, 25, 3, "35,197");

    }

    DecimalFormat df2 = new DecimalFormat(".##");

    public void printStatsD(ArrayList<Double> d, String name) {

        DescriptiveStatistics ds = new DescriptiveStatistics();
        for (int x = 0; x < d.size(); x++) {
            ds.addValue(d.get(x));
        }
        printStatsX(ds, name);
    }

    public void printStatsD(double[] d, String name) {

        DescriptiveStatistics ds = new DescriptiveStatistics();
        for (int x = 0; x < d.length; x++) {
            ds.addValue(d[x]);
        }
        printStatsX(ds, name);
    }

    public void printStatsX(DescriptiveStatistics d, String name) {

        String t = "STATS: " + name + ": Min. " + df2.format(d.getMin()) + "; Mean. " + df2.format(d.getMean());

        for (int p = 5; p < 100; p = p + 15) {
            t = t + "p" + p + " " + df2.format(d.getPercentile(p)) + "; ";
        }

        System.out.println(t + "; Max. " + df2.format(d.getMax()) + "; ");
    }

    /**
     * @return the mape
     */
    public Mapping getMape() {
        return mape;
    }

    /**
     * @param mape the mape to set
     */
    public void setMape(Mapping mape) {
        this.mape = mape;
    }

    public double calcR2(double[] x, double[] y) {
        double intercept, slope;
        double r2;
        double svar0, svar1;

        if (x.length != y.length) {
            throw new IllegalArgumentException("array lengths are not equal: " + x.length + " and " + y.length);
        }
        int n = x.length;

        // first pass
        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
        for (int i = 0; i < n; i++) {
            sumx += x[i];
            sumx2 += x[i] * x[i];
            sumy += y[i];
        }
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            yybar += (y[i] - ybar) * (y[i] - ybar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }
        slope = xybar / xxbar;
        intercept = ybar - slope * xbar;

        // more statistical analysis
        double rss = 0.0;      // residual sum of squares
        double ssr = 0.0;      // regression sum of squares
        for (int i = 0; i < n; i++) {
            double fit = slope * x[i] + intercept;
            rss += (fit - y[i]) * (fit - y[i]);
            ssr += (fit - ybar) * (fit - ybar);
        }

        int degreesOfFreedom = n - 2;
        r2 = ssr / yybar;
        double svar = rss / degreesOfFreedom;
        svar1 = svar / xxbar;
        svar0 = svar / n + xbar * xbar * svar1;

        return r2;
    }

    public double cadEstObs() {

        this.estimado = new double[71];
        this.observado = new double[71];
        contObs = 0;

        addestobs(108.609177, 245);
        addestobs(212.679558, 187);
        addestobs(1286.861075, 1304);
        addestobs(1722.628687, 1706);
        addestobs(1187.845969, 1036);
        addestobs(1178.966853, 1414);
        addestobs(2325.458785, 2123);
        addestobs(1452.434225, 1736);
        addestobs(1201.205443, 1167);
        addestobs(842.209646, 864);
        addestobs(1776.0, 1761);
        addestobs(1853.498116, 1923);
        addestobs(584.603148, 582);
        addestobs(1324.668647, 1327);
        addestobs(575.123017, 582);
        addestobs(1302.508021, 1324);
        addestobs(856.868519, 967);
        addestobs(898.246381, 774);
        addestobs(1602.695785, 1496);
        addestobs(1900.87835, 1812);
        addestobs(1824.751427, 1928);
        addestobs(2070.822343, 2011);
        addestobs(1959.321469, 2062);
        addestobs(378.697164, 328);
        addestobs(802.2981, 767);
        addestobs(34.232869, 763);
        addestobs(34.232869, 862);
        addestobs(1735.574023, 1516);
        addestobs(1925.614424, 1705);
        addestobs(1731.1, 2080);
        addestobs(1569.7409, 1590);
        addestobs(1842.308799, 1907);
        addestobs(1305.522606, 1387);
        addestobs(1411.596161, 1403);
        addestobs(1245.225003, 1256);
        addestobs(735.245389, 843);
        addestobs(1472.693622, 1486);
        addestobs(1217.243385, 1264);
        addestobs(896.07406, 872);
        addestobs(892.858499, 895);
        addestobs(1937.171917, 1926);
        addestobs(1245.110726, 1247);
        addestobs(597.361603, 523);
        addestobs(1870.865716, 1892);
        addestobs(1998.806233, 1985);
        addestobs(789.512565, 726);
        addestobs(141.33951, 127);
        addestobs(1049.916823, 1037);
        addestobs(204.472857, 211);
        addestobs(1187.845969, 1036);
        addestobs(736.618574, 709);
        addestobs(1813.137278, 1922);
        addestobs(1422.785376, 1315);
        addestobs(841.179954, 832);
        addestobs(614.288286, 888);
        addestobs(641.141483, 682);
        addestobs(1435.334664, 1426);
        addestobs(1876.615923, 1780);
        addestobs(1571.103709, 1890);
        addestobs(1629.104334, 1419);
        addestobs(1827.744944, 1849);
        addestobs(489.616551, 511);
        addestobs(924.26865, 690);
        addestobs(289.076575, 757);
        addestobs(488.425619, 748);
        addestobs(496.383419, 578);
        addestobs(1845.1, 1915);
        addestobs(2180.563217, 2021);
        addestobs(0.0, 6);
        addestobs(562.80193, 358);
        addestobs(451.626642, 421);

        GeraGraficos g = new GeraGraficos(800, 800);
        g.criarScatter("Scatter links TFlowFuzzy", "Observado", "Estimado", observado, estimado);

        return this.calcR2(estimado, observado);

    }

    int contObs;

    public void addestobs(double est, int real) {

        estimado[contObs] = est;
        observado[contObs] = real * 1.0;

        contObs++;

    }

}
