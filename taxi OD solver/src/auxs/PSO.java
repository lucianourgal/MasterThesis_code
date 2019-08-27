package auxs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import progLinear.codeGenerator;
import progLinear.network;
import taxi.od.solver.Mapping;
import taxi.od.solver.ODMatrix;
import taxi.od.solver.TaxiODSolver;
import static taxi.od.solver.TaxiODSolver.horaAtual;
import taxi.od.solver.VirtualSensors;

/**
 * * @author luciano
 * @version 5.6
 */
public class PSO {

    //ALERTA: VERIFICAR "solucao alternativa", caso função de custo esteja muito lenta
    private Output output;
    private quickSort quick;
    String nomeTeste;
    static String VirtualSensorsFile;
    private boolean salvandoBackup = false;
    private boolean sensoresFluxoNotRotas = false;
    private boolean sensoresMaisRotas = true;
    private boolean sensoresCorrelacao = false;
    private String funcaoFitness = "geh"; // geh, reg ou folga
    boolean debug = false;
    private Mapping mape;

    boolean probArestaODdefinidas = true;
    boolean warmStartup = false;
    private boolean testeRedeFechada = false;
    boolean juncaoProbArestas = false;

    int MAX_GERACOES = 1000; //200  //600  //1200
    int pop = 8000;          //60   //200  //500
    int numPart = 3750; //1250, 2500,  3750
    int it = 1500; //500,  1000,   1500

    double wIn = 0.43;
    double w;
    double wF = 0.12;
    double c1 = 0.59; //local
    double c2 = 1.39; //global
    int reset = 26;
    int contReset = 0;
    double resetPerct = 0.99;

    double CROSSOVER_PROBABILITY = 0.44; //0.44
    double MUTATION_PROBABILITY = 0.34;//0.34;
    private double varMutacao = 0.57;

    private int max, min;

    static double[][] posPart;       // posições atuais das particulas
    static double[][] velPart;    // velocidades das particulas
    static double[][] localBest;     // melhor posição já alcançada pela particula
    static int globalBest;         // Qual particula já alcançou a melhor posição
    static int N;                  // Tamanho do problema
    static double[] menorCustoLocal; // custo da melhor posição já encontrada pela particula, auxilia calculos
    private ODMatrix ODmatrix;
    // private Mapping map;
    private VirtualSensors virtualSensors;
    private int batchPriori = 0;
    private int tempoPriori;
    private int tempoProblema;
    private int batchProblema = 0;
    private int arestasSensor = -1;
    //private int[] sensorNodeFrom;
    //private int[] sensorNodeTo;
    private int[] sensorAresta;
    private int clusters;

    
    private Double[] tdd;
    private Double[] re;
    private Double[] mae;
    private Double[] rmse;
    private Double[] fitness;
    private Double[] maeIn;
    private Double[] rmseIn;
    private Double[] fitnessIn;
    private Double[] gehIn;
    private Double[] geh;
    private Double[] percGeh;
    private Double[] r2links;
    private Double[] r2odm;

    private double [][] respostas;
    
    private double[] bestMOD;
    private double fitnessBestMod;
    private boolean[] parODcoberto;

    private ArrayList<Integer>[][] ODparIndArestaMaisMov;
    private String OSMFileLocation;

    private boolean useMatrixPriori;
    private boolean useVariance;
    private ArrayList<Integer>[][] ODparIndArestaMaisMovENCONTRADOS;

    private boolean testeParametros = false;
    private int discretTemporal;

    private ArrayList<Integer> doParOD = new ArrayList<>();
    private ArrayList<Integer> daAresta = new ArrayList<>();
    private ArrayList<Integer> doIndiceSensor = new ArrayList<>();
    private ArrayList<Double> prob_od_a = new ArrayList<>();
    private int[] doParODV;
    private int[] daArestaV;
    private int[] doIndiceSensorV;
    private double[] prob_od_aV;

    private double maiorProbabilidade;

    ArrayList<String> kinds = new ArrayList<>();
    ArrayList<Double> minKind = new ArrayList<>();
    ArrayList<Double> maxKind = new ArrayList<>();
    private ArrayList<Integer>[][] ODparIndArestaMaisMov2;
    private ArrayList<Integer>[][] ODparIndArestaMaisMovENCONTRADOS2;
    private int tempoPriori2;
    private ArrayList[][] ODparIndArestaMaisMovENCONTRADOSindiceAresta;
    private ArrayList[][] ODparIndArestaMaisMovENCONTRADOS2indiceAresta;

    public long getTempoExec() {
        return tempo;
    }
    long tempo;

    public ArrayList<Double> runPSO(int runs, ODMatrix odm, VirtualSensors vts,
            int parts, int it, double c1, double c2, double wIn, double wF, int tprior, int numSensores) {

        ArrayList<Double> results = new ArrayList<>();
        ODmatrix = odm;
        tempoPriori = tprior;
        tempoProblema = tprior;
        virtualSensors = vts;
        arestasSensor = numSensores;
        long t = System.nanoTime();
        //setar parâmetros
        this.c1 = c1;
        this.c2 = c2;
        numPart = parts;
        this.it = it;
        this.wIn = wIn;
        this.wF = wF;

        runPSO(ODmatrix, virtualSensors, odm.getNumeroClusters(), tempoPriori, tempoProblema, arestasSensor, runs); //(ODmatrix, virtualSensors, clusters, tempoPriori, tempoProblema, arestasSensor, min)

        for (int r = 0; r < runs; r++) {
            results.add(geh[r]);
        }

        tempo = System.nanoTime() - t;
        return results;
    }

    public ArrayList<Double> runGA(int runs, ODMatrix odm, VirtualSensors vts,
            int pop, int geracoes, double mutationRate, double crossoverRate, int numSensores, int tempoPriorix, int tempoProbl) {
        //rnGA(ODMatrix ODmatrix1, VirtualSensors virtualSensors1,
        //int clusters, int tempoPriori, int batchPriori, int tempoProblema, int batchProblema, int arestasSensor, int runs) 
        ODmatrix = odm;
        virtualSensors = vts;
        arestasSensor = numSensores;
        tempo = System.nanoTime();

        tempoPriori = tempoPriorix;
        tempoProblema = tempoProbl;

        //setar parâmetros
        this.MAX_GERACOES = geracoes;
        this.pop = pop;
        this.MUTATION_PROBABILITY = mutationRate;
        this.CROSSOVER_PROBABILITY = crossoverRate;

        ArrayList<Double> r = runGA(ODmatrix, virtualSensors, odm.getNumeroClusters(), tempoPriori, 0, tempoProblema, 0, arestasSensor, runs);
        //(ODmatrix, virtualSensors, clusters, tempoPriori, tempoProblema, arestasSensor, min)
        tempo = System.nanoTime() - tempo;
        return r;
    }

    public void setParamPSOGA(int pop1, int it1) {
        MAX_GERACOES = it1; //200  //600  //1200
        pop = pop1;          //60   //200  //500
        numPart = pop1; //1250, 2500,  3750
        it = it1; //500,  1000,   1500
        System.out.println("ATENCAO: numPart/pop = " + pop1 + "; it/geracoes = " + it);
    }

    public void gerarCodPLGusek(int numSens, int tempoPriori1, int tempoProblema1, int batch1, VirtualSensors vt, ODMatrix odm, Mapping map, String tipoNSLP) {

        if (testeRedeFechada) {
            tempoPriori1 = 0;
            tempoProblema1 = 0;
            batch1 = 0;
            //ensoresFluxoNotRotas = true;
            //sensoresMaisRotas = false;
            //sensoresCorrelacao = false;
            if (numSens > 71) {
                numSens = 71;
                arestasSensor = 71;
                System.out.println("ATENCAO: arestasSensor reduzidas para " + arestasSensor + ", por ser numero maximo de sensores da rede cadastrada.");
            }
        }

        switch (tipoNSLP) {
            case "F":
                sensoresFluxoNotRotas = true;
                sensoresMaisRotas = true;
                break;
            case "Rmais":
                sensoresFluxoNotRotas = false;
                sensoresMaisRotas = true;
                break;
            case "Rmenos":
                sensoresFluxoNotRotas = false;
                sensoresMaisRotas = false;
                break;
            default: //Fr
                sensoresFluxoNotRotas = true;
                sensoresMaisRotas = false;
                break;
        }
        
        
        network net = new network(numSens, tempoPriori1, tempoProblema1, batch1, vt, odm, map, sensoresFluxoNotRotas, sensoresMaisRotas);
        codeGenerator cod = new codeGenerator();
        cod.setTipoNSLP(tipoNSLP);
        cod.printCodigo(net, "intv"); //min, max, intv
        if (output != null) {
            output.addText("OK: Código MILP de " + tempoPriori1 + "t" + tempoProblema1 + " "+tipoNSLP+" gerado! " + horaAtual());
        }

    }

    public String bestMODToString() {
        String s = "";
        for (int cx1 = 0; cx1 < clusters; cx1++) {
            for (int cx2 = 0; cx2 < clusters; cx2++) {
                s = s + bestMOD[cx1 * clusters + cx2] + ", ";
            }
            s = s + "\n";
        }
        return s;
    }

    public String maeRmseGehToString() {
        String s = "";
        for (int a = 0; a < mae.length; a++) {
            s = s + mae[a] + ", " + rmse[a] + ", " + geh[a] + "\n";
        }
        return s.substring(0, s.length() - 2);
    }

    public void digerirPLCadastrado(int hora, int sensors, String tipoNSLP, VirtualSensors vt, ODMatrix odm, int runs) {

        ArrayList<String> results = lerTxt("GUSEK\\resultados.txt");

        for (int r = 0; r < results.size(); r++) {
            String solution = results.get(r);
            solution = solution.replace("   ", " ");
            solution = solution.replace("  ", " ");
            String[] div = solution.split(" ");
            int tempoProblema1 = Integer.valueOf(div[1]);
            int arestasSensor1 = Integer.valueOf(div[2]);
            String tipoNSLP1 = div[3];
            

            if (tempoProblema1 == hora && sensors == arestasSensor1 && tipoNSLP.equals(tipoNSLP1)) {
                digerirResultadosPL(solution, vt, odm, runs);
                return;
            }

        }
        System.out.println("ERROR: Nao ha resultados salvos para MILP t" + hora + ", "+tipoNSLP+" e " + sensors + " sensores em GUSEK\\resultados.txt");

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

    public void digerirResultadosPL(String solution, VirtualSensors virtualSensors1, ODMatrix ODmatrix1, int runs) {

        //recuperar valores da String (tempoProblema, tempoPriori, sensores
        solution = solution.replace("   ", " ");
        solution = solution.replace("  ", " ");
        String[] div = solution.split(" ");

        tempoPriori = Integer.valueOf(div[0]);
        tempoPriori2 = tempoPriori;
        clusters = ODmatrix1.getNumeroClusters();
        //System.out.println("String " + div[0] + " vira Integer " + Integer.valueOf(div[0]));
        tempoProblema = Integer.valueOf(div[1]);
        //System.out.println("String " + div[1] + " vira Integer " + Integer.valueOf(div[1]));
        arestasSensor = Integer.valueOf(div[2]);
        //System.out.println("String " + div[2] + " vira Integer " + Integer.valueOf(div[2]));

        nomeTeste = "MILP_" + (arestasSensor) + "s_"+div[3];


        nomeTeste = nomeTeste + "_" + tempoPriori + "t" + tempoProblema;

        System.out.println("PROC: Digerindo " + nomeTeste + "... " + horaAtual());

        N = clusters * clusters;
        parODcoberto = new boolean[clusters * clusters];
        for (int c = 0; c < clusters * clusters; c++) {
            parODcoberto[c] = false;
        }

        tdd = new Double[runs];
        re = new Double[runs];
        mae = new Double[runs];
        rmse = new Double[runs];
        fitness = new Double[runs];
        maeIn = new Double[runs];
        gehIn = new Double[runs];
        geh = new Double[runs];
        percGeh = new Double[runs];
        r2links = new Double[runs];
        r2odm = new Double[runs];
        rmseIn = new Double[runs];
        fitnessIn = new Double[runs];

        this.virtualSensors = virtualSensors1;
        this.ODmatrix = ODmatrix1;
        ODmatrix.calcMatrixClust();
        ODmatrix.calcVarianciaODMatrix();

        virtualSensors.calcVarianciaArestas();
        //this.clusters = clusters;

        definirArestas_NSLP();

        descobrirODparIndArestaMaisMov(); //ODparIndAresta(indice geral) -> (indice vetor sensorAresta)
        

        if (!probArestaODdefinidas) {
            /*this.definirVariaveisProbabilidadeArestaRota(clusters);
            posPart = new double[numPart][N + doParOD.size()];
            velPart = new double[numPart][N + doParOD.size()];
            localBest = new double[numPart][N + doParOD.size()];
            virtualSensors = encontrarMinMaxPorTipoAresta(virtualSensors);*/
        } else {
            definirAssignmentMatrix2(tempoPriori);
            posPart = new double[numPart][N];
            velPart = new double[numPart][N];
            localBest = new double[numPart][N];
        }

        double[][] sensores = new double[arestasSensor][1];
        localBest = new double[20][N];
        //this.iniciaParts(0,0, false);

        minInicio = this.getMinutosAtual();

        String tempo = horaAtual().replace(",", "") + ", " + runs + ", ";
        String resumo = 0 + "," + 0 + ", " + N + ", " + arestasSensor + ", ";

        System.out.println("OK: Iniciando MILP Digest!" + resumo + nomeTeste + "; " + horaAtual());

        //preencher matrizA  (probabilidade de uso de aresta)
        /*for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) {
                for (int ars = 0; ars < ODparIndArestaMaisMov[o][d].size(); ars++) {
                    if (ODparIndArestaMaisMovENCONTRADOS[o][d].contains(ars)) {
                        matrizA[ODparIndArestaMaisMov[o][d].get(ars)][o * clusters + d]
                                = ODmatrix.getODParArestaCont(o, d, tempoPriori, ars);
                    }
                }
            }
        }*/
        //preencher matriz de sensores  (quantidade por sensor)
        for (int a = 0; a < arestasSensor; a++) {
            sensores[a][0] = virtualSensors.getContArestaBatch(sensorAresta[a], tempoProblema, batchProblema);
        }

        bestMOD = new double[clusters * clusters];

        //avalia qualidade da solução
        globalBest = 0;
        System.out.println("localBest[].lenght = " + localBest[globalBest].length + "; div[].lenght = " + div.length);
        for (int a = 0; a < N; a++) {
            localBest[globalBest][a] = Double.valueOf(div[4 + a]);
            posPart[globalBest][a] = Double.valueOf(div[4 + a]);
            bestMOD[a] = Double.valueOf(div[4 + a]);
        }

        
        geh[0] = this.calcGEH(true);
        gehIn[0] = geh[0];
        percGeh[0] = this.percGehAbaixo5;
        r2links[0] = r2Global;
        r2odm[0] = this.calcR2Odm();
        rmse[0] = this.calcRMSE();
        rmseIn[0] = rmse[0];
        mae[0] = this.calcMAE();
        maeIn[0] = mae[0];
        fitness[0] = 0.0;
        fitnessIn[0] = 0.0;
        tdd[0] = this.calcTDDodm();
        re[0] = this.calcREodm();
        
        for(int c=1;c<mae.length;c++){
            
        tdd[c] = tdd[0];
        re[c] = re[0];
        geh[c] = geh[0];
        gehIn[c] = gehIn[0];
        percGeh[c] = percGeh[0];
        r2links[c] = r2links[0];
        r2odm[c] = r2odm[0];
        rmse[c] = rmse[0];
        rmseIn[c] = rmseIn[0];
        mae[c] = mae[0];
        maeIn[c] = maeIn[0];
        fitness[c] = 0.0;
        fitnessIn[c] = 0.0;
        }
            
        

        //salva resultado
        minInicio = (((double) getMinutosAtual()) - minInicio) / ((double) runs);
        tempo = tempo + horaAtual().replace(",", "") + ", " + minInicio;
        System.out.println("RUN  " + nomeTeste + " " + (1) + "/" + runs + printMetricasFinais(0) + " \n");

        resultsAlgoritmos rest = new resultsAlgoritmos();
        rest = rest.recuperarArquivo();
        rest.addResultados(nomeTeste, arestasSensor, rmse, mae, fitness, geh,
                rmseIn, maeIn, fitnessIn, gehIn, bestMOD, tempo + ", " + resumo, r2links, r2odm, percGeh, tdd, re);
        rest.salvarArquivo(true);
        output.addText("OK: Solução MILP de " + tempoPriori + "t" + tempoProblema + " digerida! " + horaAtual());

    }

    private String printMetricasFinais(int rodada) {
        return " (RMSE: " + rmse[rodada] + "; MAE: " + mae[rodada] + "; GEH: " + geh[rodada] + " (" + percGeh[rodada] + "); R2Links: " + r2links[rodada] + "; R2Odm: " + r2odm[rodada] + ")! " + horaAtual().replace(",", "");
    }

    public void outputDispose() {
        output.dispose();
    }

    public void setOutputName(String name) {
        output.setName(name);
    }

    public PSO(String VirtualSensorsFil, int discretT) {
        VirtualSensorsFile = VirtualSensorsFil;
        discretTemporal = discretT;
    }

    public void setParam(int nPart, int iterat) {
        numPart = nPart;
        it = iterat;
    }

    /* private void definirAssignmentMatrix(int t1, int t2) {

        //sensorAresta;
        for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) //para todos os pares OD
            {
                for (int ars = 0; ars < ODparIndArestaMaisMovENCONTRADOS[o][d].size(); ars++) {

                    doParOD.add(o * clusters + d);
                    //daAresta.add(ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars));
                    int ax = -1;
                    for (int x = 0; x < arestasSensor; x++) //sensorAresta tem indices das arestas mais movimentadas. Se encontrar tal aresta no vetor arestas do par OD
                    {
                        if (sensorAresta[x] == ODmatrix.getODparArestaIndexGeralAresta(o, d, discretTemporal, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))) {
                            ax = x;
                            x = arestasSensor; //
                        }
                    }

                    daAresta.add(ax);    // daAresta é o índice para fluxosSens. Do tipo: 0 para a aresta mais movimentada. 1 para a segunda mais movimentada
                    prob_od_a.add(ODmatrix.getODParArestaCont(o, d, discretTemporal, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)));

                }
            }
        }

        System.out.println("INFO: Existem " + prob_od_a.size() + " relações Prob_OD_aresta!");

    }*/
    private void definirAssignmentMatrix2(int t1) {

        if (clusters == 0) {
            System.out.println("ERROR: Numero de clusters indefinido! (definirAssignmentMatrix2)");
        }
        if (sensorAresta.length == 0) {
            System.out.println("ERROR: Vetor de sensores indefinido! (definirAssignmentMatrix2)");
        }

        doParOD = new ArrayList<>();
        doIndiceSensor = new ArrayList<>();
        daAresta = new ArrayList<>();
        prob_od_a = new ArrayList<>();

        //sensorAresta;
        for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) //para todos os pares OD
            {
                for (int ars = 0; ars < sensorAresta.length; ars++) {

                    if (ODmatrix.getProbUsoArestaPorParODeIndexAresta(o, d, t1, sensorAresta[ars]) > 0.0) {

                        doParOD.add(o * clusters + d);
                        daAresta.add(sensorAresta[ars]);
                        doIndiceSensor.add(ars);
                        prob_od_a.add(ODmatrix.getProbUsoArestaPorParODeIndexAresta(o, d, t1, sensorAresta[ars]));

                    }

                }
            }
        }

        doParODV = new int[doParOD.size()];
        doIndiceSensorV = new int[doParOD.size()];
        daArestaV = new int[doParOD.size()];
        prob_od_aV = new double[doParOD.size()];

        for (int x = 0; x < doParOD.size(); x++) {
            doParODV[x] = doParOD.get(x);
            doIndiceSensorV[x] = doIndiceSensor.get(x);
            daArestaV[x] = daAresta.get(x);
            prob_od_aV[x] = prob_od_a.get(x);
        }

        System.out.println("INFO: Existem " + prob_od_a.size() + " relações Prob_OD_aresta!");

    }

    private VirtualSensors encontrarMinMaxPorTipoAresta(VirtualSensors vt) {

        System.out.println("PROC: Calculando faixas de valores para tipo de aresta..." + horaAtual());

        ArrayList<DescriptiveStatistics> sts = new ArrayList<>();
        ArrayList<String> kinds = new ArrayList<>();
        int index;

        //passa por todas as arestas
        for (int a = 0; a < vt.getFromNodCod().size(); a++) {

            index = kinds.indexOf(vt.getArestaKind(a));

            //se não existe esse tipo cadastrado
            if (index == -1) {

                kinds.add(vt.getArestaKind(a));
                sts.add(new DescriptiveStatistics());
                index = kinds.size() - 1;

            }//else{    
            //se já existe esse tipo cadastrado
            // }

            //para todos os pares O-D que passam pela aresta
            for (int o = 0; o < clusters; o++) {
                for (int d = 0; d < clusters; d++) {
                    if (ODparIndArestaMaisMovENCONTRADOS[o][d].contains(a))//se par OD passa pela aresta A
                    {
                        sts.get(index).addValue(ODmatrix.getODParArestaCont(o, d, tempoPriori, a)); //adiciona porcentagem da aresta A
                    }
                }
            }
            vt.setIndiceArestaKind(a, index);

        } // fim do laço de arestas

        for (int k = 0; k < kinds.size(); k++) { //encontra 
            minKind.add(sts.get(k).getPercentile(1));
            maxKind.add(sts.get(k).getPercentile(99));
        }

        vt.salvarDat(VirtualSensorsFile);

        return vt;
    }

    private void definirVariaveisProbabilidadeArestaRota(int clusters) {

        //ODmatrix.getODParArestaCont(o, d, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))
        for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) {
                //por par origem destino
                DescriptiveStatistics dx = new DescriptiveStatistics();
                for (int ars = 0; ars < ODparIndArestaMaisMovENCONTRADOS[o][d].size(); ars++) {
                    dx.addValue(ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)));
                }

                double minimo = dx.getPercentile(25); //define mínimo para ser relevante

                for (int ars = 0; ars < ODparIndArestaMaisMovENCONTRADOS[o][d].size(); ars++) {
                    if (ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)) > minimo) {  //se fator da aresta

                        doParOD.add(o * clusters + d);

                        int ax = -1;
                        for (int x = 0; x < arestasSensor; x++) //sensorAresta tem indices das arestas mais movimentadas. Se encontrar tal aresta no vetor arestas do par OD
                        {
                            if (sensorAresta[x] == ODmatrix.getODparArestaIndexGeralAresta(o, d, discretTemporal, ODparIndArestaMaisMov[o][d].get(ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)))) {
                                ax = x;
                                x = arestasSensor; //
                            }
                        }

                        daAresta.add(ax);
                        //daAresta.add(ODparIndArestaMaisMov[o][d].get(ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)));

                        if (ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)) > maiorProbabilidade) {
                            maiorProbabilidade = ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars));
                        }

                    }
                }

            }
        }

        System.out.println("OK: Total de " + doParOD.size() + " probabilidades de aresta/par_o-d");
    }

    public void encontrarParametrosPSO(ODMatrix ODmatrix1, VirtualSensors virtualSensors1,
            int clusters, int tempoPriori, int batchPriori, int tempoProblema, int batchProblema, int arestasSensor, int runs) {

        testeParametros = true;
        runs = 3;
        String melhoresPar = "";
        double melhorMedia = 0;
        double aux;
        double prop;
        int teste = 1;

        ArrayList<Double> wInV = new ArrayList<>();
        ArrayList<Double> wFV = new ArrayList<>();
        ArrayList<Double> c1V = new ArrayList<>();
        ArrayList<Double> c2V = new ArrayList<>();
        ArrayList<Double> propPGV = new ArrayList<>();

        ArrayList<Double> pFracV = new ArrayList<>();
        ArrayList<Double> pReduxV = new ArrayList<>();
        ArrayList<Integer> pMinV = new ArrayList<>();

        pMinV.add(8);
        pMinV.add(20);
        pMinV.add(50);
        pMinV.add(100);
        pMinV.add(200);
        //pMinV.add(300);
        pMinV.add(400);

        pFracV.add(0.1);
        pFracV.add(0.08);
        pFracV.add(0.07);
        pFracV.add(0.075);
        //pFracV.add(0.065);
        pFracV.add(0.06);
        pFracV.add(0.05);
        pFracV.add(0.04);
        pFracV.add(0.2);
        pFracV.add(0.3);
        pFracV.add(0.75);
        pFracV.add(0.8);
        //pFracV.add(0.85);
        pFracV.add(0.9);
        //pFracV.add(0.95);

        pReduxV.add(0.14);
        pReduxV.add(0.10);
        pReduxV.add(0.09);
        pReduxV.add(0.08);
        pReduxV.add(0.05);
        pReduxV.add(0.03);

        wInV.add(0.4);
        //wInV.add(0.3);
        //wFV.add(0.1);
        //wFV.add(0.15);
        wFV.add(0.2);

        //c1V.add(0.2);
        //c1V.add(0.3);
        c1V.add(0.4);

        //c2V.add(0.2);
        //c2V.add(0.3);
        c2V.add(0.4);

        //propPGV.add(1000.0/600.0);
        propPGV.add(1000.0 / 400.0);
        //propPGV.add(1000.0 / 300.0);
        //propPGV.add(1000.0/200.0);

        System.out.println("PROC: Iniciando seleção de parametros. " + (pFracV.size() * pReduxV.size() * wInV.size() * wFV.size() * c1V.size() * c2V.size() * propPGV.size()));

        for (int pMinx = 0; pMinx < pMinV.size(); pMinx++) {
            for (int pFracx = 0; pFracx < pFracV.size(); pFracx++) {
                for (int pReduxx = 0; pReduxx < pReduxV.size(); pReduxx++) {
                    for (int wInx = 0; wInx < wInV.size(); wInx++) {
                        for (int wFx = 0; wFx < wFV.size(); wFx++) {
                            for (int c1x = 0; c1x < c1V.size(); c1x++) {
                                for (int c2x = 0; c2x < c2V.size(); c2x++) {
                                    for (int px = 0; px < propPGV.size(); px++) {

                                        wIn = wInV.get(wInx);
                                        wF = wFV.get(wFx);
                                        c1 = c1V.get(c1x);
                                        c2 = c2V.get(c2x);
                                        prop = propPGV.get(px);
                                        pFrac = pFracV.get(pFracx);
                                        pRedux = pReduxV.get(pReduxx);

                                        //setMin((int) pMinV.get(pMinx));
                                        setMax(pMinV.get(pMinx));

                                        //numPart * it < limite
                                        // numPart/it = prop
                                        it = 100;
                                        numPart = 1;
                                        while (numPart * it < 18000) {

                                            numPart = (int) (it * prop);
                                            it += 100;
                                        }
                                        it = it - 100;

                                        aux = this.runPSO(ODmatrix1, virtualSensors1, clusters, tempoPriori, tempoProblema, 2000, 5);

                                        if (teste == 1) {
                                            melhoresPar = "pMin = " + min + "; pFrac =" + pFrac + "; pRedux =" + pRedux + "; wIn=" + wIn + "; wF=" + wF + "; c1=" + c1 + "; c2=" + c2 + "; numPart=" + numPart + "; t=" + it + "; melhorMedia=" + aux;
                                            melhorMedia = aux;
                                        } else if (aux < melhorMedia) {
                                            melhoresPar = "pMin = " + min + "; pFrac =" + pFrac + "; pRedux =" + pRedux + ";wIn=" + wIn + "; wF=" + wF + "; c1=" + c1 + "; c2=" + c2 + "; numPart=" + numPart + "; t=" + it + "; melhorMedia=" + aux;
                                            melhorMedia = aux;
                                        }

                                        teste++;

                                        System.out.println("TESTE " + teste + ": melhorParametros: " + melhoresPar + "\n\n");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("END! melhoresParametros: " + melhoresPar);
    }

    public void setUseMatrixPriori(boolean b) {
        useMatrixPriori = b;
    }

    private double minInicio;

    public void testBatchesOptions(ODMatrix ODmatrix) {

        int sum;
        clusters = ODmatrix.getNumeroClusters();
        N = clusters * clusters;
        double[] respMOD = new double[N];

        for (int t = 6; t < 19; t++) {
            for (int b = 0; b < 6; b++) {
                for (int o = 0; o < clusters; o++) {
                    for (int d = 0; d < clusters; d++) {

                        respMOD[o * clusters + d] = ODmatrix.getODMatrixClustersBatch(o, d, t, b);

                    }
                }

                sum = 0;
                for (int i = 0; i < N; i++) {
                    sum += respMOD[i];
                }
                System.out.println("T=" + t + "; B=" + b + "> Sum: " + sum + ";Med " + ((double) sum / (double) N));

            }
        }

    }

    public void runMultipleAlgebricSolution(ODMatrix ODmatrix1, VirtualSensors virtualSensors1,
            int clusters, int tempoPriori, int tempoProblema, int arestasSensorI, int arestasSensorF, int step, String pasta, int runs) {

        GeraGraficos g = new GeraGraficos();
        ArrayList<Double>[] data = new ArrayList[1];
        data[0] = new ArrayList<>();
        ArrayList<Double> labelValues = new ArrayList<>();
        ArrayList<String> labelSeries = new ArrayList<>();
        labelSeries.add("GLS");

        for (int x = arestasSensorI; x <= arestasSensorF; x += step) {
            data[0].add(runAlgebricSolution(ODmatrix1, virtualSensors1, clusters, tempoPriori, tempoProblema, x, runs));
            labelValues.add((double) x);
        }

        g.GeraGraficosLinha(pasta, "GLS MAE vs Number of Sensors", data, labelValues, labelSeries, "Sensors number", "MAE Error");

    }

    boolean algebParaWarmUp = false;

    public double runAlgebricSolution(ODMatrix ODmatrix1, VirtualSensors virtualSensors1,
            int clusters, int tempoPriori, int tempoProblema, int arestasSensor, int runs) {

        if (testeRedeFechada) {
            tempoPriori = 0;
            tempoProblema = 0;
            batchProblema = 0;
            batchPriori = 0;
            
            sensoresFluxoNotRotas = true;
            sensoresMaisRotas = false;
            sensoresCorrelacao = false;
            
            if (arestasSensor > 71) {
                arestasSensor = 71;
                System.out.println("ATENCAO: arestasSensor reduzidas para " + arestasSensor + ", por ser numero maximo de sensores da rede cadastrada.");
            }

        }

        if (!algebParaWarmUp) {
            nomeTeste = "LS_" + (arestasSensor) + "s_";
        }

        this.tempoPriori = tempoPriori;
        this.tempoPriori2 = tempoPriori;
        this.clusters = clusters;
        this.tempoProblema = tempoProblema;
        this.arestasSensor = arestasSensor;
        ODmatrix = ODmatrix1;

        useVariance = false;

        if (!algebParaWarmUp) {
            if (sensoresFluxoNotRotas) {
                if(sensoresMaisRotas)
                nomeTeste = nomeTeste + "_F";
                else
                    nomeTeste = nomeTeste + "_Fr";
            } else {
                if(sensoresMaisRotas)
                    nomeTeste = nomeTeste + "_Rmais";
                else
                    nomeTeste = nomeTeste + "_Rmenos";
            }
            
            if(sensoresCorrelacao)
                nomeTeste = nomeTeste + "C";
            
            
            nomeTeste = nomeTeste + "_" + tempoPriori + "t" + tempoProblema;
        }

        System.out.println("PROC: Preparando para iniciar " + nomeTeste + "... " + horaAtual());

        N = clusters * clusters;
        parODcoberto = new boolean[clusters * clusters];
        for (int c = 0; c < clusters * clusters; c++) {
            parODcoberto[c] = false;
        }

        if (!algebParaWarmUp) {
            
            tdd = new Double[runs];
            re = new Double[runs];
            mae = new Double[runs];
            rmse = new Double[runs];
            fitness = new Double[runs];
            maeIn = new Double[runs];
            gehIn = new Double[runs];
            geh = new Double[runs];
            percGeh = new Double[runs];
            r2links = new Double[runs];
            r2odm = new Double[runs];
            rmseIn = new Double[runs];
            fitnessIn = new Double[runs];
        }

        this.virtualSensors = virtualSensors1;
        ODmatrix.calcMatrixClust();
        ODmatrix.calcVarianciaODMatrix();
        //ODmatrix.normalizarPathLinkMatrix(); - ja normalizado na criação
        //ODmatrix.printStatsVariancia(tempoProblema);
        //ODmatrix.redefinirODparArestaContAPartirDeVirtualSensors(virtualSensors, tempoProblema, batchProblema);
        //this.map = map1;

        virtualSensors.calcVarianciaArestas();
        // virtualSensors.printStatsVariancia(tempoProblema, arestasSensor);

        
               
        definirArestas_NSLP();

        descobrirODparIndArestaMaisMov(); //ODparIndAresta(indice geral) -> (indice vetor sensorAresta)
      

        if (!probArestaODdefinidas) {
            this.definirVariaveisProbabilidadeArestaRota(clusters);
            posPart = new double[numPart][N + doParOD.size()];
            //velPart = new double[numPart][N + doParOD.size()];
            localBest = new double[numPart][N + doParOD.size()];
            virtualSensors = encontrarMinMaxPorTipoAresta(virtualSensors);
        } else {
            definirAssignmentMatrix2(tempoPriori);
            posPart = new double[numPart][N];
            // velPart = new double[numPart][N];
            localBest = new double[numPart][N];
        }

        double[][] matrizA = new double[arestasSensor][clusters * clusters];
        double[][] matrizAT;
        double[][] matrizx;// = new double[clusters*clusters][1];
        double[][] matrizATxB;
        double[][] inv_MaATxMa;
        double[][] sensores = new double[arestasSensor][1];
        localBest = new double[numPart][N];
        //this.iniciaParts(0,0, false);

        matrixUtilities matr = new matrixUtilities();
        if (!algebParaWarmUp) {
            minInicio = this.getMinutosAtual();
        }

        String tempo = horaAtual().replace(",", "") + ", ";
        String resumo = 0 + "," + 0 + ", " + N + ", " + arestasSensor + ", ";

        System.out.println("OK: Iniciando Algeb!" + resumo + nomeTeste + "; " + horaAtual());

        //preencher matrizA  (probabilidade de uso de aresta)
        //for(int a=0;a<arestasSensor;a++)
        for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) {

                /*for (int ars = 0; ars < ODparIndArestaMaisMovENCONTRADOS[o][d].size(); ars++) 
                        matrizA[ODparIndArestaMaisMov[o][d].get(ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))][o*clusters+d] = 
                                ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars));*/
                for (int ars = 0; ars < ODparIndArestaMaisMov[o][d].size(); ars++) {
                    if (ODparIndArestaMaisMovENCONTRADOS[o][d].contains(ars)) {
                        matrizA[ODparIndArestaMaisMov[o][d].get(ars)][o * clusters + d]
                                = ODmatrix.getODParArestaCont(o, d, tempoPriori, ars);
                    }
                }

            }
        }
        //preencher matriz de sensores  (quantidade por sensor)
        for (int a = 0; a < arestasSensor; a++) {
            sensores[a][0] = virtualSensors.getContArestaBatch(sensorAresta[a], tempoProblema, batchProblema);
        }

        //realiza cálculos        
        // System.out.println("INFO Tam matrizB-sensores = "+sensores.length +" x "+sensores[0].length);
        // System.out.println("INFO Tam matrizA = "+matrizA.length +" x "+matrizA[0].length);
        matrizAT = matr.inverteMatriz(matrizA);
        // System.out.println("INFO Tam matrizAT = "+matrizAT.length +" x "+matrizAT[0].length);
        matrizATxB = matr.mult(matrizAT, sensores);
        // System.out.println("INFO Tam matrizATxB = "+matrizATxB.length +" x "+matrizATxB[0].length);
        inv_MaATxMa = matr.inverteMatriz(matr.mult(matrizAT, matrizA));
        // System.out.println("INFO Tam inv_MaATxMa = "+inv_MaATxMa.length +" x "+inv_MaATxMa[0].length);
        matrizx = matr.mult(inv_MaATxMa, matrizATxB);
        //  System.out.println("INFO Tam matrizx = "+matrizx.length +" x "+matrizx[0].length);

        bestMOD = new double[clusters * clusters];

        //avalia qualidade da solução
        globalBest = 0;
        for (int a = 0; a < N; a++) {
            localBest[globalBest][a] = matrizx[a][0];
            posPart[globalBest][a] = matrizx[a][0];
            if(matrizx[a][0] >0 && parODcoberto[a])
            bestMOD[a] = matrizx[a][0];
            else
                bestMOD[a] = 0;
        }

        if (!algebParaWarmUp) {
            rmse[0] = this.calcRMSE();
            rmseIn[0] = rmse[0];
            mae[0] = this.calcMAE();
            maeIn[0] = mae[0];
            tdd[0] = this.calcTDDodm();
            re[0] = this.calcREodm();
            geh[0] = this.calcGEH(true);
            gehIn[0] = geh[0];
            percGeh[0] = this.percGehAbaixo5;
            r2links[0] = r2Global;
            r2odm[0] = calcR2Odm();
            fitness[0] = 0.0;
            fitnessIn[0] = 0.0;
        }
        
        
        for(int c=1;c<mae.length;c++){
            
        tdd[c] = tdd[0];
        re[c] = re[0];
        geh[c] = geh[0];
        gehIn[c] = gehIn[0];
        percGeh[c] = percGeh[0];
        r2links[c] = r2links[0];
        r2odm[c] = r2odm[0];
        rmse[c] = rmse[0];
        rmseIn[c] = rmseIn[0];
        mae[c] = mae[0];
        maeIn[c] = maeIn[0];
        fitness[c] = 0.0;
        fitnessIn[c] = 0.0;
        }
        

        //salva resultado
        if (!algebParaWarmUp) {
            minInicio = (((double) getMinutosAtual()) - minInicio);
            tempo = tempo + horaAtual().replace(",", "") + ", " + minInicio;
            resultsAlgoritmos rest = new resultsAlgoritmos();
            rest = rest.recuperarArquivo();
            rest.addResultados(nomeTeste, arestasSensor, rmse, mae, fitness, geh,
                    rmseIn, maeIn, fitnessIn, gehIn, bestMOD, tempo + ", " + resumo, r2links, r2odm, percGeh, tdd, re);
            rest.salvarArquivo(true);

            String tex = "RUN  " + nomeTeste + this.printMetricasFinais(0) + " \n";
            System.out.println(tex);
            output.addText(tex);
            return mediaVet(mae);
        }
        return -1;

    }

    resultsAlgoritmos algs;
    
    public int carregarDadosAnteriores(int totalRuns) {
        int inicio = 0;
        algs = new resultsAlgoritmos();
        algs = algs.recuperarArquivo();
        int contAnterior = algs.getContIncompleto(nomeTeste);
        int runAnterior = algs.getIndexRunAnterior(nomeTeste);
        if (contAnterior >= 0) {
            inicio = contAnterior;
            
            tdd = algs.getTdd().get(runAnterior);
            re = algs.getRe().get(runAnterior);
            mae = algs.getMae().get(runAnterior);
            rmse = algs.getRmse().get(runAnterior);
            fitness = algs.getFitness().get(runAnterior);
            maeIn = algs.getMaeInicial().get(runAnterior);
            gehIn = algs.getGehInicial().get(runAnterior);
            geh = algs.getGeh().get(runAnterior);
            percGeh = algs.getPercGehAbaixo5().get(runAnterior);
            r2links = algs.getR2links().get(runAnterior);
            r2odm = algs.getR2odm().get(runAnterior);
            rmseIn = algs.getRmseInicial().get(runAnterior);
            fitnessIn = algs.getFitnessInicial().get(runAnterior);
            bestMOD = algs.getFlatMatrizOD(runAnterior);
            fitnessBestMod = algs.getMenorCusto(runAnterior);
            System.out.println("OK: Continuando de onde parou na última execução! ("+inicio+"->" + (inicio+1) + "/" + totalRuns + ")");
            output.addText("OK: Continuando de onde parou na última execução! ("+inicio+"->" + (inicio+1) + "/" + totalRuns + ")");
        }else if(contAnterior == -2){
            System.out.println("ALERT: '"+nomeTeste+ "' não encontrado em resultsAlgs.dat! Iniciando pela 1° rodada.");
            //return -1;
        }else if(contAnterior == -1){
            System.out.println("OK: Existe "+nomeTeste+" em resultsAlgs.dat e já está completo!");
            return totalRuns+2;
        }
            
        return inicio;
    }

    public double runPSO(ODMatrix ODmatrix1, VirtualSensors virtualSensors1,
            int clusters, int tempoPriori, int tempoProblema, int arestasSensor1, int runs) {

        // Output output = new Output();
        
        respostas = new double[clusters*clusters][runs];
        
        arestasSensor = arestasSensor1;
        if (testeRedeFechada) {
            System.out.println("OK: Mudando teste para rede fechada...");
            tempoPriori = 0;
            tempoProblema = 0;
            batchPriori = 0;
            batchProblema = 0;
            sensoresFluxoNotRotas = true;
            sensoresMaisRotas = false;
            sensoresCorrelacao = false;
            if (arestasSensor > 71) {
                arestasSensor = 71;
                System.out.println("ATENCAO: arestasSensor reduzidas para " + arestasSensor + ", por ser numero maximo de sensores da rede cadastrada.");
            }
        }

        if(funcaoFitness.equals("reg"))        
            nomeTeste = "PSO_" + (arestasSensor) + "s_";
        else if(funcaoFitness.equals("geh"))        
            nomeTeste = "PSOg_" + (arestasSensor) + "s_";
        else if(funcaoFitness.equals("folga"))        
            nomeTeste = "PSOf_" + (arestasSensor) + "s_";
        /*if (useMatrixPriori) {
            nomeTeste = nomeTeste + "P1";
        } else {
            nomeTeste = nomeTeste + "P0";
        }
        if (useVariance) {
            nomeTeste = nomeTeste + "_V1";
        } else {
            nomeTeste = nomeTeste + "_V0";
        }*/
        if (sensoresFluxoNotRotas) {
            if(sensoresMaisRotas)
            nomeTeste = nomeTeste + "_F";
            else
                nomeTeste = nomeTeste + "_Fr";
        } else {
            if(sensoresMaisRotas)
                nomeTeste = nomeTeste + "_Rmais";
            else
                nomeTeste = nomeTeste + "_Rmenos";
        }

        if(sensoresCorrelacao)
            nomeTeste = nomeTeste + "C";
        
        nomeTeste = nomeTeste + "_" + tempoPriori + "t" + tempoProblema;

        System.out.println("PROC: Preparando para iniciar " + nomeTeste + "... " + horaAtual());
        output.addText("PROC: Preparando para iniciar " + nomeTeste + "... " + horaAtual());
        output.setNome(nomeTeste);

        N = clusters * clusters;
        parODcoberto = new boolean[clusters * clusters];
        for (int c = 0; c < clusters * clusters; c++) {
            parODcoberto[c] = false;
        }

        tdd = new Double[runs];
        re = new Double[runs];
        mae = new Double[runs];
        rmse = new Double[runs];
        fitness = new Double[runs];
        maeIn = new Double[runs];
        geh = new Double[runs];
        gehIn = new Double[runs];
        percGeh = new Double[runs];
        r2links = new Double[runs];
        r2odm = new Double[runs];
        rmseIn = new Double[runs];
        fitnessIn = new Double[runs];

        this.virtualSensors = virtualSensors1;
        this.ODmatrix = ODmatrix1;
        ODmatrix.calcMatrixClust();
        ODmatrix.calcVarianciaODMatrix();
        //ODmatrix.normalizarPathLinkMatrix(); - ja normalizado na criação
        //ODmatrix.printStatsVariancia(tempoProblema);
        //ODmatrix.redefinirODparArestaContAPartirDeVirtualSensors(virtualSensors, tempoProblema, batchProblema);
        //this.map = map1;

        virtualSensors.calcVarianciaArestas();
        // virtualSensors.printStatsVariancia(tempoProblema, arestasSensor);
        this.tempoPriori = tempoPriori;
        this.tempoPriori2 = tempoPriori;
        //this.batchPriori = batchPriori;
        this.tempoProblema = tempoProblema;
        //this.batchProblema = batchProblema;
        //this.arestasSensor = arestasSensor; above
        this.clusters = clusters;

        definirArestas_NSLP();
       
        descobrirODparIndArestaMaisMov();

        if (!probArestaODdefinidas) {
            /*  this.definirVariaveisProbabilidadeArestaRota(clusters);
            posPart = new double[numPart][N + doParOD.size()];
            velPart = new double[numPart][N + doParOD.size()];
            localBest = new double[numPart][N + doParOD.size()];
            virtualSensors = encontrarMinMaxPorTipoAresta(virtualSensors);*/
        } else {
            definirAssignmentMatrix2(tempoPriori);
            posPart = new double[numPart][N];
            velPart = new double[numPart][N];
            localBest = new double[numPart][N];
        }

        //inicia matrizes
        menorCustoLocal = new double[numPart];

        minInicio = this.getMinutosAtual();

        String tempo = horaAtual().replace(",", "") + ", " + runs + ", ";
        String resumo = "s=" + this.numPart + ", t=" + this.it + ", OD=" + N + ", sensores=" + arestasSensor + ", ";

        int inicio = 0;

        if (salvandoBackup && !testeParametros) {

            inicio = carregarDadosAnteriores(runs);

        }

        Double[] evo = new Double[it];
        Double[] evo2 = new Double[it];
        ArrayList[] data = new ArrayList[runs];
        ArrayList[] data2 = new ArrayList[runs];
        ArrayList<String> labelSeries = new ArrayList<>();
        ArrayList<Double> labelValues = new ArrayList<>();

        //determinação de parametros
        System.out.println("OK: Iniciando PSO! " + resumo + nomeTeste + "; " + horaAtual());
        output.addText(horaAtual() + ": OK: Iniciando PSO! " + resumo + nomeTeste + "; ");

        for (int r = inicio; r < runs; r++) {   ///INICIO DE RODADA

            labelSeries.add("r" + r);
            int aux, aux2;
            double aux3, menorCusto;

            /*if (tempoPriori != tempoProblema) {
                //batchPriori = (int) (Math.random() * 6);
                //batchProblema = batchPriori;
                batchPriori = 0;
                batchProblema = batchPriori;
            } else {
                batchPriori = (int) (Math.random() * 6);
                batchProblema = batchPriori;
                while (batchProblema == batchPriori) {
                    batchPriori = (int) (Math.random() * 6);
                }
            }*/
            //inicia velocidades randomicamente
            for (aux = 0; aux < numPart; aux++) {
                for (aux2 = 0; aux2 < N; aux2++) {
                    velPart[aux][aux2] = (((max - min) / 2) - ((max - min) * Math.random())) / 2;  //min + ((max - min) * Math.random() / 10);
                }
                if (!probArestaODdefinidas) {
                    for (aux2 = N; aux2 < (N + doParOD.size()); aux2++) {
                        velPart[aux][aux2] = (0.1) - (Math.random() * 0.2);  //min + ((max - min) * Math.random() / 10);
                    }
                }
            }

            //inicia posições e atribui melhor local   
            iniciaParts(min, max, false);

            for (aux = 0; aux < numPart; aux++) {
                System.arraycopy(posPart[aux], 0, localBest[aux], 0, N);  //LocalBest é posição inicial da partícula
                menorCustoLocal[aux] = calculaCusto(aux, false, false);
            }

            //determina global best
            globalBest = 0;
            menorCusto = menorCustoLocal[0];

            for (aux = 1; aux < numPart; aux++) {
                aux3 = menorCustoLocal[aux]; //calculaCustoGLS(aux);
                if (aux3 < menorCusto) {
                    globalBest = aux;
                    menorCusto = aux3;
                }
            }

            double maeI, rmseI, gehI, fI;

            maeI = calcMAE();//maeIn[r] = calcMAE();
            rmseI = calcRMSE();//rmseIn[r] = calcRMSE();
            gehI = this.calcGEH(false);//gehIn[r] = this.calcGEH(false);
            fI = menorCusto;//fitnessIn[r] = menorCusto;

            System.out.println("RUN " + nomeTeste + " " + (r + 1) + "/" + runs + ": Menor custo inicial é " + menorCusto + "(RMSE: " + rmseI + "; MAE: " + maeI + "; GEH: " + gehI + ") p=" + globalBest);
            int a = 0;

            while (a < it) {  //ou outra condição de parada

                evo[a] = calcGEH(false);
                evo2[a] = menorCusto;

                w = wIn + (wF - wIn) * ((it - a) / it);

                //movimenta particulas
                for (aux = 0; aux < numPart; aux++) {
                    for (aux2 = 0; aux2 < N; aux2++) {
                        //if (!ODparIndArestaMaisMov[(aux2 - (aux % clusters)) / clusters][aux2 % clusters].isEmpty()) // int id2 = x % clusters; int id1 = (x - id2) / clusters;
                        if (parODcoberto[aux2]) { //somente se par OD é utilizado

                            posPart[aux][aux2] = posPart[aux][aux2] + (/*(int)*/velPart[aux][aux2]);

                            
                            //reparação de solução
                            if (posPart[aux][aux2] < 0) {
                                posPart[aux][aux2] = /*(int)*/ (-posPart[aux][aux2] / 4);
                            }
                            else if (posPart[aux][aux2] < 0.1) {
                                posPart[aux][aux2] = 0;
                            }else if (posPart[aux][aux2] > max*2) {
                                posPart[aux][aux2] = max*1.5;
                            }
                            
                        }
                    }

                    if (!probArestaODdefinidas) {
                        for (int ax = N; ax < (N + doParOD.size()); ax++) {

                            posPart[aux][ax] = posPart[aux][ax] + (/*(int)*/velPart[aux][aux2]);

                            int k = virtualSensors.getIndiceArestaKind(daAresta.get(ax - N));
                            double min1 = minKind.get(k);
                            double max1 = maxKind.get(k);

                            if (posPart[aux][ax] < min1) {
                                posPart[aux][ax] = min1;
                            } else if (posPart[aux][ax] > max1) {
                                posPart[aux][ax] = max1;
                            }

                        }
                    }

                }

                //recalcula local e global best  
                for (aux = 0; aux < numPart; aux++) {

                    aux3 = calculaCusto(aux, false, false);

                    if (aux3 <= menorCustoLocal[aux]) { // É localBest
                        for (aux2 = 0; aux2 < N; aux2++) {
                            localBest[aux][aux2] = posPart[aux][aux2];
                        }
                        menorCustoLocal[aux] = aux3; //calculaCustoGLS(aux);

                        if (aux3 < menorCusto) { // É globalBest
                            globalBest = aux;
                            menorCusto = aux3;
                        }
                    }
                }

                contReset++;
                if (contReset == reset) {
                    iniciaParts(min, max, true);
                    contReset = 0;
                    //System.out.print(".");
                }

                //calcula novas velocidades
                for (aux = 0; aux < numPart; aux++) {
                    for (aux2 = 0; aux2 < N; aux2++) {
                        if (parODcoberto[aux2]) {
                            velPart[aux][aux2]
                                    = w * velPart[aux][aux2]
                                    + c1 * (localBest[aux][aux2] - posPart[aux][aux2]) * Math.random()
                                    + c2 * (localBest[globalBest][aux2] - posPart[aux][aux2]) * Math.random();
                        }
                    }

                    if (!probArestaODdefinidas) {
                        for (int ax = N; ax < (N + doParOD.size()); ax++) {

                            velPart[aux][aux2]
                                    = w * velPart[aux][ax]
                                    + c1 * (localBest[aux][ax] - posPart[aux][ax]) * Math.random()
                                    + c2 * (localBest[globalBest][ax] - posPart[aux][ax]) * Math.random();
                        }
                    }
                }

                a++;
            } //fim do laço principal

            if (salvandoBackup && !testeParametros) {

                r = carregarDadosAnteriores(runs);
                
            }

            if (r < runs) {
                data[r] = vetorToArray(evo);
                data2[r] = vetorToArray(evo2);
                maeIn[r] = maeI;
                mae[r] = calcMAE();
                tdd[r] = this.calcTDDodm();
                re[r] = this.calcREodm();
                rmseIn[r] = rmseI;
                rmse[r] = calcRMSE();
                gehIn[r] = gehI;
                geh[r] = calcGEH(r==2);
                percGeh[r] = this.percGehAbaixo5;
                fitnessIn[r] = fI;
                fitness[r] = menorCustoLocal[globalBest];
                r2links[r] = r2Global;
                r2odm[r] = calcR2Odm();
                
                for(int c=0;c<(clusters*clusters);c++)
                    respostas[c][r] = localBest[globalBest][c];
                
                
            } else {
                System.out.println("WARNING: Resultados descartados. Vetores de resultados ja estao completos.");
                return 666.0;
            }

            if (r == 0) {
                fitnessBestMod = menorCusto;
                bestMOD = localBest[globalBest];
            } else if (menorCusto < fitnessBestMod) {
                fitnessBestMod = menorCusto;
                bestMOD = localBest[globalBest];
            }

            System.out.println("RUN " + nomeTeste + " " + (r + 1) + "/" + runs + ": Menor  custo  final é " + menorCusto + printMetricasFinais(r) + " ´p=" + globalBest + "\n");
            output.addText(horaAtual().replace(",", "") + ": RUN " + nomeTeste + " " + (r + 1) + "/" + runs + printMetricasFinais(r));
            
            if (salvandoBackup && r != (runs - 1) && !testeParametros) {
                //resultsAlgoritmos algs = new resultsAlgoritmos();
                //algs = algs.recuperarArquivo();
                algs.addPartialResults(nomeTeste, arestasSensor1, rmse, mae, fitness, geh,
                        rmseIn, maeIn, fitnessIn, gehIn, bestMOD, tempo, (r), r2links, r2odm, percGeh, tdd, re);
                algs.salvarArquivo(false);
            }       

        } //fim da RUN   ( r < runs )


        System.out.println(statsRespostas());
        
        if (!testeParametros) {
            minInicio = (((double) getMinutosAtual()) - minInicio) / ((double) runs);
            tempo = tempo + horaAtual().replace(",", "") + ", " + minInicio;

            resultsAlgoritmos rest = new resultsAlgoritmos();
            rest = rest.recuperarArquivo();
            rest.addResultados(nomeTeste, arestasSensor1, rmse, mae, fitness, geh,
                    rmseIn, maeIn, fitnessIn, gehIn, localBest[globalBest], tempo + ", " + resumo, r2links, r2odm, percGeh, tdd, re);
            System.out.println("ALERT: Salvando ultima MOD resposta. Nao a melhor.");

            double[] respMOD = new double[N];
            for (int o = 0; o < clusters; o++) {
                for (int d = 0; d < clusters; d++) {
                    respMOD[o * clusters + d] = ODmatrix.getODMatrixClustersBatch(o, d, tempoProblema, batchProblema);
                }
            }
            Double vet[] = new Double[1];
            vet[0] = 0.0;
            //rest.addResultados("MOD_t" + tempoProblema + "b" + batchProblema, 0, vet, vet, vet, vet, vet, vet, vet, vet, respMOD, "", vet, vet, vet);

            rest.salvarArquivo(true);   //gera boxplot ao salvar arquivo
        }

        output.addText(horaAtual() + ": OK!! " + nomeTeste + " CONCLUIDO!");
        output.setConcluido();
        
        if(!salvandoBackup){
        
         for (int a = 0; a < it; a++) {
            labelValues.add(a * 1.0);
        }
       
        /*GeraGraficos gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha("Graficos", "Evolution PSO GEH", data, labelValues, labelSeries, "Iterations (s=" + numPart + ";t=" + it + ")", "Fitness (" + nomeTeste + ")");
        gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha("Graficos", "Evolution PSO F", data2, labelValues, labelSeries, "Iterations (s=" + numPart + ";t=" + it + ")", "Fitness (" + nomeTeste + ")");*/
        return mediaVet(geh);
        
        }
        
        return 666.0;
        
    }

    
    public String statsRespostas(){
        System.out.println("PROC: PSO.statsRespostas");
     String s = "";   
        
    for(int c=0;c<(clusters*clusters);c++){
    
        
        int id2 = c % clusters;
        int id1 = (c - id2) / clusters;
        
        DescriptiveStatistics d = new DescriptiveStatistics();
        for(int r=0;r<respostas[c].length;r++)
            d.addValue(respostas[c][r]);
        
        s = s + "Par "+c+". Real "+ ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema)  +  "; Min "+d.getMin()+"; Medna "+d.getPercentile(50)+"; Max "+d.getMax()+"\n";
    
    }    
    
    return s;
    }
    
    
    public static ArrayList<Double> vetorToArray(Double[] ve) {
        ArrayList<Double> d = new ArrayList<>();
        d.addAll(Arrays.asList(ve));
        return d;
    }

    public double mediaVet(Double[] vet) {
        double med = 0;
        for (int a = 0; a < vet.length; a++) {
            med = med + vet[a];
        }
        return med / vet.length;
    }

    double pFrac = 0.075;
    double pRedux = 0.1;

    public void iniciaParts(double min, double max, boolean reset) {

        double r1 = Math.random();
        double frac = pFrac; //* Math.random()
        double redux = pRedux;  //* Math.random()

        double offset = 0;//(max - min) * 0.4 * r1;
        double offset2 = 0;//(max - min) * (0.4 + (r1 * 0.4)) * Math.random();

        for (int p = 0; p < posPart.length; p++) {
            if (!reset || (reset && p != globalBest && (Math.random()>this.resetPerct))) //iniciar part SE (não for reset) OU se for reset, se não for globalBest e com 70% de chance
            {
                for (int x = 0; x < N; x++) {

                    //if (!ODparIndArestaMaisMov[(x - (x % clusters)) / clusters][x % clusters].isEmpty())
                    if (parODcoberto[x]) {
                        posPart[p][x] = (min + offset + (Math.random() * (max - min - offset2)));
                        if (Math.random() > frac) {
                            posPart[p][x] = posPart[p][x] * redux;
                        }

                    } else {
                        posPart[p][x] = 0;
                    }
                    //   if(Math.random()>=0.8 && p!=globalBest){
                    //       localBest[p] = posPart[p];
                    //      menorCustoLocal[p] = calculaCustoGLS(p);
                    //  }                 
                }
            }

            if (!probArestaODdefinidas) {
                double min1, max1;
                for (int x = N; x < (N + doParOD.size()); x++) {
                    int k = virtualSensors.getIndiceArestaKind(daAresta.get(x - N));
                    min1 = minKind.get(k);
                    max1 = maxKind.get(k);
                    posPart[p][x] = min1 + (max1 - min1) * Math.random();        //(Math.random()*maiorProbabilidade*0.7);
                }
            }

            if (p < posPart.length/10) { // para uso de matrix priori  OU warmStartUp

                /*if (warmStartup && !reset) {
                    algebParaWarmUp = true;
                    //roda algeb
                    this.runAlgebricSolution(ODmatrix, virtualSensors,
                            clusters, tempoPriori, tempoProblema, arestasSensor, 0);
                    //algeb ja deixa resultado na particula 0
                    algebParaWarmUp = false;
                }*/

                if (useMatrixPriori && !reset) {
                    for (int x = 0; x < N; x++) {
                        int id2 = x % clusters;
                        int id1 = (x - id2) / clusters;
                        if(Math.random() > 0.22)
                            posPart[p][x] = ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori);
                        // posPart[p][x] = ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema);
                    }
                }
            }

        }

    }

    
    
    public double calcREodm() {

        int id1, id2;
        double LocalRE = 0.0;
        double somaTrips = 0.0;

        for (int n = 0; n < N; n++) { //soma das diferencas ao quadrado
            id2 = n % clusters;
            id1 = (n - id2) / clusters;
            LocalRE += ((localBest[globalBest][n] - ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema))/ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema)) 
                    * (localBest[globalBest][n] - ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema))/ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema);
            somaTrips += ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema);
        }

        //dividido por N, raiz quadrada
        LocalRE = Math.sqrt(LocalRE / 2);
        //somaTrips = somaTrips / N;

        return 100 * (LocalRE);
    }
    
    
        public double calcTDDodm() {

        int id1, id2;
        double LocalTDD1 = 0.0;
        double LocalTDD2 = 0.0;
        

        for (int n = 0; n < N; n++) { //soma das diferencas ao quadrado
            id2 = n % clusters;
            id1 = (n - id2) / clusters;
            LocalTDD1 += (localBest[globalBest][n]);
            LocalTDD2 += (ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema));
        }

        //dividido por N, raiz quadrada
        LocalTDD1 = Math.sqrt((LocalTDD1 - LocalTDD2)*(LocalTDD1 - LocalTDD2));
        //somaTrips = somaTrips / N;

        return 100 * (LocalTDD1/LocalTDD2);
    }
    
    
    
    
    
    
    public double calcRMSE() {

        int id1, id2;
        double LocalRMSE = 0.0;
        double somaTrips = 0.0;

        for (int n = 0; n < N; n++) { //soma das diferencas ao quadrado
            id2 = n % clusters;
            id1 = (n - id2) / clusters;
            LocalRMSE += (localBest[globalBest][n] - ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema))
                    * (localBest[globalBest][n] - ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema));
            somaTrips += ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema);
        }

        //dividido por N, raiz quadrada
        LocalRMSE = Math.sqrt(LocalRMSE / N);
        somaTrips = somaTrips / N;

        return 100 * (LocalRMSE / somaTrips);
    }

    public double calcGEH(boolean gerarScatter) {

        for (int x = 0; x < N; x++) {
            posPart[globalBest][x] = localBest[globalBest][x];
        }

        this.calculaCusto(globalBest, true, gerarScatter);
        return gehGlobal;

    }

    public double calcMAE() {

        int cmais = 0, cmenos = 0;
        DescriptiveStatistics d = new DescriptiveStatistics();

        int id1, id2;
        double LocalMAE = 0.0;

        for (int n = 0; n < N; n++) { //soma das diferencas ao quadrado
            id2 = n % clusters;
            id1 = (n - id2) / clusters;

            if (localBest[globalBest][n] > ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema)) {
                cmais++;
                d.addValue(localBest[globalBest][n] - ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema));
            } else if (localBest[globalBest][n] < ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema)) {
                cmenos++;

            }

            LocalMAE += Math.sqrt((localBest[globalBest][n] - ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema))
                    * (localBest[globalBest][n] - ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema)));                //modulo da diferença por par OD
        }

        //System.out.println("MAE: Para mais em " + cmais + ", para menos em " + cmenos + "; Mean p+: " + d.getMean() + "; p50 p+: " + d.getPercentile(50) + "; p5 p+: " + d.getPercentile(5) + "; p95 p+: " + d.getPercentile(95));
        return LocalMAE / N;         //sobre número de pares OD
    }

    int ultimaMelhoriaGA;

    public double calculaCustoGA(int p, int ger) {

        double f = calculaCusto(p, false, false);

        double f2 = (10000.0 / (f));
        //double f2 = 250000 - f;

        /*for (int mults = 0; mults < 1; mults++) {
            f2 = f2 * 10000.0 / f;
        }*/
 /*double f = calculaCusto(p, true, false);
        double f2;
        if(gehGlobal<10)
        f2 = 10* (10 - gehGlobal);
        else
            f2 = 0;*/
        if (melhorFitness < f2) {
            melhorFitness = f2;
            ultimaMelhoriaGA = ger;
        } else if (f2 < 0) {
            System.out.println("ALERT: Fitness < 0 = " + f2 + "; (calculaCustoGA)");
            f2 = 0;
        }

        return f2;

    }

    public double calculaCusto(int p, boolean calcGeh, boolean salvarScater) {

        if (probArestaODdefinidas) {
            return calculaCustoComProbArestas(p, calcGeh, salvarScater);
        } else {
            return calculaCustoSemProbArestas(p);
        }

    }

    public double calculaCustoSemProbArestas(int part) {

        /*if (debug && part == 0) {System.out.println("PROC: Iniciando calculo de fitness... " + horaAtual());}*/
        double custoSoma1 = 0.0;
        double custoSoma2 = 0.0;
        int id1, id2;
        int tamanhoODPar = doParOD.size();
        //int tempoProri, int batchPriori, int tempoProblema, int batchProblema;
        double[] fluxosSens = new double[arestasSensor];
        //sensorAresta[], sensorNodeFrom[], sensorNodeTo[]

        //por todos pares ARESTA/PAR_O-D, adiciona no fluxo da aresta o Ti do par_OD * probabilidade de uso da aresta pelo par o-d
        for (int z = 0; ++z < tamanhoODPar;) {
            fluxosSens[daAresta.get(z)] += posPart[part][doParOD.get(z)] * posPart[part][N + z];

            //if(posPart[part][doParOD.get(z)] * posPart[part][N+z] < 0)
            //   System.out.println("<0 = posPart[part]["+doParOD.get(z)+"]= "+ posPart[part][doParOD.get(z)]+" *  posPart[part][N+"+z+"] = "+ posPart[part][N+z] );
            /*if(daAresta.get(z)==3604){
                        System.out.println("("+co++ +", +"+posPart[part][doParOD.get(z)] * posPart[part][N+z]+")fluxosSens[3604] = "+fluxosSens[3604]);
                    }*/
        }


        /*if (debug && part == 0) {
            System.out.println("OK: Calculou fluxosSens " + horaAtual());
        }*/
        //   stats(fluxosSens);
        //todos os pontos OD  (estimado - priori)^2
        if (useMatrixPriori) {
            /*for (int n = 0; n < N; n++) {
                id2 = n % clusters;
                id1 = (n - id2) / clusters;

             
                if (useVariance) {
                    custoSoma1 += ((posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))
                            * (posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori))))
                            / (ODmatrix.getODMatrixClusterVariance(id1, id2, tempoProblema) * 1000);
                } else {
                    custoSoma1 += ((posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))
                            * (posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))) / 1000;
                }

            }*/

        }

        DescriptiveStatistics dx = new DescriptiveStatistics();
        //todos os fluxos  (variancia aresta Tproblema)*(consequência - virtualSensors)^2
        for (int ar = 0; ar < arestasSensor; ar++) {

            /*if(part==0){
                System.out.println("(fluxosSens["+ar+"] - vtSnr.getContArestaBatch(sensorAresta["+ar+"], tPriori, bPriori)) ^2 ="
                        + " ("+fluxosSens[ar]+" - "+virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori)+")^2  "
                                + " = " + ((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori))* (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori)) ));             
            } */
            if (useVariance) {
                custoSoma2 += mod(((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema))
                        * (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema)))
                        / (virtualSensors.getArestaVariance(sensorAresta[ar], tempoProblema) * 1000));
            } else {
                custoSoma2 += mod(((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema))
                        * (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema))) / 1000);
            }

            if (fluxosSens[ar] < 0) {
                System.out.println("ERRO: fluxoSens[" + ar + "]=" + fluxosSens[ar]);
            }
            if (virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema) < 0) {
                System.out.println("ERRO: virtualSensors.getContArestaBatch(sensorAresta[" + ar + "])=" + fluxosSens[ar]);
            }

            //   if((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori))
            //             * (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori))
            //           /   ( virtualSensors.getArestaVariance(sensorAresta[ar], tempoProblema) * 10000) <0)
            if (/*part == 0 &&*/debug) {
                //  System.out.println("DIF Fluxos: ("+fluxosSens[ar]+"-"+virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori)+")^2 / "+virtualSensors.getArestaVariance(sensorAresta[ar], tempoProblema));
                dx.addValue(fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori));
            }
        }

        if (debug/* && part == 0*/) {
            printStats(dx, "Desvio fluxos");
            //System.out.println("OK: Calculou erros de fluxosSens. END " + horaAtual());
            //debug = false;
        }
        // System.out.println("(p="+part+") ODM E:  "+(custoSoma1/N)+"; Fluxos E: "+(custoSoma2/arestasSensor)+" ps. mudar linha 280");
        //return (custoSoma1 / N + custoSoma2 / arestasSensor);
        return (custoSoma1 + custoSoma2);
    }

    double percGehAbaixo5;

    public double calculaCustoComProbArestas(int part, boolean calcGeh, boolean gerarScatter) {

        /*if (debug && part == 0) {System.out.println("PROC: Iniciando calculo de fitness... " + horaAtual());}*/
        double custoSoma1 = 0.0;
        double custoSoma2 = 0.0;
        double gehX = 0.0;
        double m, c;
        int id1, id2;
        int probs = doParOD.size();
        // calcGeh = true;

        //int tempoProri, int batchPriori, int tempoProblema, int batchProblema;
        double[] fluxosSens = new double[arestasSensor];
        //sensorAresta[], sensorNodeFrom[], sensorNodeTo[]
        double[] real = new double[arestasSensor];

        int reserva = -1;
        if (juncaoProbArestas) {
            reserva = tempoPriori;
            tempoPriori = discretTemporal;
        }

        //solucao alternativa 
        if (!calcGeh) {
            for (int z = 0; ++z < probs;) {
                //fluxosSens[daAresta.get(z)] += posPart[part][doParOD.get(z)] * prob_od_a.get(z);
                //fluxosSens[doIndiceSensor.get(z)] += posPart[part][doParOD.get(z)] * prob_od_a.get(z);
                fluxosSens[doIndiceSensorV[z]] += posPart[part][doParODV[z]] * prob_od_aV[z];
            }
        } else {
            percGehAbaixo5 = 0;
            for (int z = 0; ++z < probs;) {
                //fluxosSens[daAresta.get(z)] += posPart[part][doParOD.get(z)] * prob_od_a.get(z);
                //fluxosSens[doIndiceSensor.get(z)] += posPart[part][doParOD.get(z)] * prob_od_a.get(z);
                if (posPart[part][doParODV[z]] > 0) {
                    fluxosSens[doIndiceSensorV[z]] += posPart[part][doParODV[z]] * prob_od_aV[z];
                }
            }
        }
        //(vetor maiores arestas, não do indice geral de arestas)
        /*for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) //para todos os pares OD
            {
                //for (int ars = 0; ars < ODparIndArestaMaisMov[o][d].size(); ars++) //percorre array de arestas que par OD passa

                  for (int ars = 0; ars < ODparIndArestaMaisMovENCONTRADOS[o][d].size(); ars++) {

                    fluxosSens[ODparIndArestaMaisMov[o][d].get(ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))]
                            += (((double) posPart[part][o * clusters + d])
                            * ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))); //aresta acrescentada pela part*fator(aresta)   

                    if (((double) posPart[part][o * clusters + d])
                            * ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)) < 0) {
                        System.out.println("fluxosSens[" + ODparIndArestaMaisMov[o][d].get(ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)) + "] += "
                                + "" + posPart[part][o * clusters + d] + " * " + ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)) + " "
                                + "== " + (((double) posPart[part][o * clusters + d]) * ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))));
                    }

                    //System.out.println("fluxosSens["+ODparIndArestaMaisMov[o][d].get(ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))+"] += "
                    //      + ""+posPart[part][o * clusters + d]+" * " + ODmatrix.getODParArestaCont(o, d, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars) ) +  " "
                    //               + "== "+(((double) posPart[part][o * clusters + d]) * ODmatrix.getODParArestaCont(o, d, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars))));                    
                } 
            }
        }*/
        if (juncaoProbArestas) {
            tempoPriori = reserva;
        }

        /*if (debug && part == 0) {
            System.out.println("OK: Calculou fluxosSens " + horaAtual());
        }*/
        //   stats(fluxosSens);
        //todos os pontos OD  (estimado - priori)^2
        if (useMatrixPriori) {
            /*for (int n = 0; n < N; n++) {
                id2 = n % clusters;
                id1 = (n - id2) / clusters;

                double dif = mod(posPart[part][n] -  ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori))/ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)+0.001;
                
               // if(part==0){  //debug tempo real
                    System.out.println("((posPart[0]["+n+"] - ODmatrix.getODMatrixClustersBatch("+id1+", "+id2+", tPriori, bPriori))^2  = ("+posPart[part][n]+" - "+ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)+")^2 =  "+ ((posPart[part][n] - ((double)ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))
                            * (posPart[part][n] - ((double)ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))));                    
                } 
                if (useVariance) {
                    custoSoma1 += ((posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))
                            * (posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori))))
                            / (ODmatrix.getODMatrixClusterVariance(id1, id2, tempoProblema) * 1000);
                } else if (dif < 0.5 || dif > 2)  {
                    custoSoma1 += 1;// mod(dif-1);
                            
                            //((posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))
                            //* (posPart[part][n] - ((double) ODmatrix.getODMatrixClustersBatch(id1, id2, tempoPriori, batchPriori)))) / 1000;
                }

            } */

        }

        //GEH = SQRT ( 2(M-C)^2 / (M+C) )
        DescriptiveStatistics dx = new DescriptiveStatistics();
        double auz;
        int contAZ = 0;
        double folga = 0;
        
        //todos os fluxos  (variancia aresta Tproblema)*(consequência - virtualSensors)^2
        for (int ar = 0; ar < arestasSensor; ar++) {

            /*if(part==0){
                System.out.println("(fluxosSens["+ar+"] - vtSnr.getContArestaBatch(sensorAresta["+ar+"], tPriori, bPriori)) ^2 ="
                        + " ("+fluxosSens[ar]+" - "+virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori)+")^2  "
                                + " = " + ((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori))* (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori)) ));             
            } */
            if (useVariance) {
                custoSoma2 += mod(((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema))
                        * (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema)))
                        / (virtualSensors.getArestaVariance(sensorAresta[ar], tempoProblema) * 1000));
            } else if(getFuncaoFitness().equals("reg")) {
                custoSoma2 += mod(((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema))
                        * (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema))) / 1000);
            }

            if (calcGeh || getFuncaoFitness().equals("geh")) {

                m = fluxosSens[ar];
                c = virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema);

                if ((m + c) > 0) {
                    auz = Math.sqrt((2 * (m - c) * (m - c)) / (m + c));
                    gehX = gehX + auz;
                    if (auz < 5.0) {
                        percGehAbaixo5++;
                    }
                    contAZ++;
                }
                real[ar] = c;

            } else if (getFuncaoFitness().equals("folga")){
                
                folga = folga + mod(fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema));
            
            }

            if (fluxosSens[ar] < 0) {
                System.out.println("ERRO: fluxoSens[" + ar + "]=" + fluxosSens[ar]);
            }
            if (virtualSensors.getContArestaBatch(sensorAresta[ar], tempoProblema, batchProblema) < 0) {
                System.out.println("ERRO: virtualSensors.getContArestaBatch(sensorAresta[" + ar + "])=" + fluxosSens[ar]);
            }

            //   if((fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori))
            //             * (fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori))
            //           /   ( virtualSensors.getArestaVariance(sensorAresta[ar], tempoProblema) * 10000) <0)
            if (part == 0 && debug) {
                //  System.out.println("DIF Fluxos: ("+fluxosSens[ar]+"-"+virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori)+")^2 / "+virtualSensors.getArestaVariance(sensorAresta[ar], tempoProblema));
                dx.addValue(fluxosSens[ar] - virtualSensors.getContArestaBatch(sensorAresta[ar], tempoPriori, batchPriori));
            }
        }

        if (debug && part == 0) {
            printStats(dx, "Desvio fluxos");
            //System.out.println("OK: Calculou erros de fluxosSens. END " + horaAtual());
            debug = false;
        }
        // System.out.println("(p="+part+") ODM E:  "+(custoSoma1/N)+"; Fluxos E: "+(custoSoma2/arestasSensor)+" ps. mudar linha 280");
        //return (custoSoma1 / N + custoSoma2 / arestasSensor);
        
        
        if(testeRedeFechada && gerarScatter){
        if(ger==null){    
            ger = new geradordeRede2();
           
        }
        ger.setMape(mape);
        ger.setFluxosParaBPR(fluxosSens);
        //ger.estatisticasEngarrafamentoVias(fluxosSens, sensorAresta); 
        }
        
        
        if (calcGeh || getFuncaoFitness().equals("geh")) {
            gehGlobal = gehX / ((double) arestasSensor);
            r2Global = this.calcR2(fluxosSens, real);
            percGehAbaixo5 = percGehAbaixo5 / contAZ;

            
            if (gerarScatter) {

                /*for(int i=0;i<fluxosSens.length;i++){
                    if(real[i]>220 ||  fluxosSens[i]>220){
                        real[i]=0;
                        fluxosSens[i]=0;
                    }
                }*/
                GeraGraficos g = new GeraGraficos(600, 600);
                g.criarScatter("Scatter Links " + nomeTeste, "Observed Link Counts", "Estimated Link Counts", real, fluxosSens);//filename, xlabel, ylabel, 

                //gerar Scatter ODM
                String odmLegivel = "http://www.darrinward.com/lat-long/\n\n";
                double[] est = new double[N];
                real = new double[N];
                for (int a = 0; a < (N); a++) {
                    
                    est[a] = localBest[globalBest][a];
                    id2 = a % clusters;
                    id1 = (a - id2) / clusters;
                    
                    if(localBest[globalBest][a]>0)
                        odmLegivel = odmLegivel + localBest[globalBest][a]+" = OD "+id1+" to "+id2+"\n";
                    real[a] = ODmatrix.getODMatrixClustersBatch(id1, id2, tempoProblema, batchProblema);
                }
                g.criarScatter("Scatter ODM " + nomeTeste, "Observed ODM Counts", "Estimated ODM Counts", real, est);//filename, xlabel, ylabel, 
                salvarTxt("demonstra_rotas\\ODM "+nomeTeste+".txt",odmLegivel);
            }
            
            if(getFuncaoFitness().equals("geh"))
                return gehGlobal*(1+custoSoma1/200);
            
            // System.out.println("GEH: "+gehX/((double)arestasSensor)+" = "+gehX+"/"+arestasSensor);
        }
        
        if (getFuncaoFitness().equals("folga"))
            return folga*(1+custoSoma1/200);
        
        //  return gehGlobal;
        return (custoSoma2)*(1+custoSoma1/200);
    }

    double gehGlobal;
    double r2Global;
    geradordeRede2 ger;
    
    private double mod(double d) {

        if (d < 0) {
            return -d;
        } else {
            return d;
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
    
    
    public void descobrirODparIndArestaMaisMov() {
        ODparIndArestaMaisMov = new ArrayList[clusters][clusters];
        ODparIndArestaMaisMovENCONTRADOS = new ArrayList[clusters][clusters];

        ODparIndArestaMaisMov2 = new ArrayList[clusters][clusters];
        ODparIndArestaMaisMovENCONTRADOS2 = new ArrayList[clusters][clusters];

        int cont = 0;
        int cob = 0;
        int reserva = -1;
        if (juncaoProbArestas) {
            reserva = tempoPriori;
            tempoPriori = discretTemporal;
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

        if (juncaoProbArestas) {
            tempoPriori = reserva;
        }

        System.out.println("INFO: descobrirODparIndArestaMaisMov = " + cont + " casos! - " + cob + "/" + (clusters * clusters) + " pares O-D cobertos");
    }

    public String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));
    }

    public int getMinutosAtual() {
        int d = Integer.valueOf(new SimpleDateFormat("dd").format(Calendar.getInstance().getTime()));
        int h = Integer.valueOf(new SimpleDateFormat("HH").format(Calendar.getInstance().getTime()));
        int m = Integer.valueOf(new SimpleDateFormat("mm").format(Calendar.getInstance().getTime()));
        //System.out.println("d="+d+"; h"+h+"; m="+m+" sum: "+(60*24*d + 60*h + m));
        return 60 * 24 * d + 60 * h + m;
    }

    private void stats(int[] fluxosSens) {
        DecimalFormat df2 = new DecimalFormat(".##");
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int f = 0; f < fluxosSens.length; f++) {
            stats.addValue(fluxosSens[f]);
        }

        System.out.println("STATS: FluxosSensores (vehicles). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

    }

    /**
     * @param useVariance the useVariance to set
     */
    public void setUseVariance(boolean useVariance) {
        this.useVariance = useVariance;
    }

    //GA GA GA GA GA GA GA
    // Parametros do GA
    //int[][] Populacao = new int[pop * 2][15];
    double[] Fitness;
    double melhorFitness;
    int[] pai;
    int[] maeGA;
//int contadorAvaliacoes;
    int[] ordem;

    private int Evolucao = 0;
    int ResultadoPorRodada = 0;

    boolean casaisDinamicos = false;
//1 para sim, em que filhos recem gerados possam ser pais (maior desempenho, similar versão 1.8), 0 para que não (visão mais tradicional)

    //int [] fitg = new int[MAX_GERACOES];  
    private String texto;
    private String seqOtima;

    public void escolhaPais(int g) {

        int cont;
        double soma = 0.0;
        double menorFitness = 0.0;//Fitness[ordem[pop - 1]] * 0.95;

        for (cont = 0; cont <= (pop - 1); cont++) {
            soma += (Fitness[ordem[cont]] - menorFitness);        //soma dos fitness da populaçao
        }
        //System.out.println("("+g+")Menor Fitness = "+Fitness[ordem[pop-1]]+"; Melhor fitness = "+ Fitness[ordem[0]]);

        for (cont = 0; cont <= ((pop / 2) - 1); cont++) {
            pai[cont] = GARoleta(soma, menorFitness);
            maeGA[cont] = GARoleta(soma, menorFitness);
        }

    }

    public int GARoleta(double soma, double menorFitness) {
        int aux, GARoleta1 = (int) (Math.random() * (pop - 1));
        double sorteado = soma * Math.random();    //define ponto de sorteio
        //System.out.println("Soma="+soma+"; sorteado="+sorteado+" best="+Fitness[ordem[0]]+"; worst ="+Fitness[ordem[pop-1]]);
        soma = 0.0;

        for (aux = 0; aux <= (pop - 1); aux++) {
            soma += (Fitness[ordem[aux]] - menorFitness);

            if (soma >= sorteado) {     //se chegou no ponto, retorna o indice
                //System.out.println("GARoleta: "+aux+"° colocado. Index "+ordem[aux]);
                return aux;//ordem[aux];
            }
        }
        //System.out.println("ERROR: Não conseguiu definir GARoleta");
        return GARoleta1;
    }

    public int GARoletaOriginal() {
        int aux, GARoleta1 = 0;
        double soma = 0, sorteado;

        for (aux = 0; aux <= (pop - 1); aux++) {
            soma = Fitness[ordem[aux]] + soma;        //soma dos fitness da populaçao
        }

        sorteado = soma * Math.random();    //define ponto de sorteio
        soma = 0;

        for (aux = 0; aux <= (pop - 1); aux++) {
            soma = soma + Fitness[ordem[aux]];

            if (soma > sorteado) {     //se chegou no ponto, retorna o indice
                return aux;
                //return ordem[aux] ??????
            }

        }
        System.out.println("ERROR: Não conseguiu definir GARoleta");
        return GARoleta1;
    }

    public double Roleta(int min, int max) {

        if (max > min) {
            return (Math.random() * (max - min)) + min;
        } else {
            System.out.println("ERROR: min(" + min + ")!<max(" + max + ") PSO.Roleta()");
            return 0;
        }

    }

    public void setParamGA(int populacao, int geracoes, double crossover, double mutacao, double varMut) {

        this.pop = populacao;
        this.MAX_GERACOES = geracoes;
        this.CROSSOVER_PROBABILITY = crossover;
        this.MUTATION_PROBABILITY = mutacao;
        this.varMutacao = varMut;
        System.out.println("GA PARAM: pop = " + pop + "; ger = " + geracoes + " (" + (pop * geracoes) + " avaliacoes); Crossover = " + crossover + "; Mutacao = " + mutacao + "; varMut = " + varMut);
    }

    public void setParamPSO(int numP, int iterat, double wIn, double wF, double c1, double c2, int itReset, double pReset) {

        this.numPart = numP;
        this.it = iterat;
        this.wIn = wIn;
        this.wF = wF;
        this.c1 = c1;
        this.c2 = c2;
        this.reset = itReset;
        this.resetPerct = pReset;

        System.out.println("PSO PARAM: numPart = " + numP + "; Iterat = " + it + " (" + (numP * it) + " avaliacoes); Win = " + wIn + "; Wf = " + wF + "; c1 = " + c1 + "; c2 = " + c2);

    }

    
    public void definirArestas_NSLP(){
        
    if(sensoresCorrelacao)
        arestasSensor = arestasSensor*2;
        
        
    if (sensoresFluxoNotRotas) {
            if(sensoresMaisRotas) //apenas por maiores fluxos
                virtualSensors.getArestasMaismovimentadasIndex(arestasSensor, tempoPriori);
            else   // por fluxos/rotas
                virtualSensors.getArestasMaismovimentadasMenosRotasIndex(arestasSensor, tempoPriori, ODmatrix);
        } else {
            if(sensoresMaisRotas)
                virtualSensors.getArestasMaisRotasIndex(ODmatrix, arestasSensor, tempoPriori);
            else
                virtualSensors.getArestasMenosRotasIndex(ODmatrix, arestasSensor, tempoPriori);
        }
        //sensorNodeTo = virtualSensors.getArestasMaismovimentadasIndexTONODE();
        sensorAresta = virtualSensors.getArestasMaismovimentadasIndexARESTA();   


         if(sensoresCorrelacao){
            arestasSensor = arestasSensor/2;
            lerArquivoOSMDat();
            sensorAresta = mape.calcNSLPcorrelacao(arestasSensor, sensorAresta, ODmatrix);//calcNSLPcorrelacao(int qtdeFinal, int [] arestasOpcao, ODMatrix odm)
            
         }
          
    }
    
    
    
    public ArrayList<Double> runGA(ODMatrix ODmatrix1, VirtualSensors virtualSensors1,
            int clusters, int tempoPriori, int batchPriori, int tempoProblema, int batchProblema, int arestasSensor, int runs) {
        double somaSolucoes, pior = 0, melhor = 0;
        int rodada;
        ArrayList<Double> results = new ArrayList<>();
        quick = new quickSort();

        if (testeRedeFechada) {
            tempoPriori = 0;
            tempoProblema = 0;
            batchProblema = 0;
            batchPriori = 0;
            sensoresFluxoNotRotas = true;
            sensoresMaisRotas = false;
            sensoresCorrelacao = false;
            if (arestasSensor > 71) {
                arestasSensor = 71;
                System.out.println("ATENCAO: arestasSensor reduzidas para " + arestasSensor + ", por ser numero maximo de sensores da rede cadastrada.");
            }
        }

        
        if(funcaoFitness.equals("reg"))        
            nomeTeste = "GA_" + (arestasSensor) + "s_";
        else if(funcaoFitness.equals("geh"))        
            nomeTeste = "GAg_" + (arestasSensor) + "s_";
        else if(funcaoFitness.equals("folga"))        
            nomeTeste = "GAf_" + (arestasSensor) + "s_";
        
        /*if (useMatrixPriori) {
            nomeTeste = nomeTeste + "P1";
        } else {
            nomeTeste = nomeTeste + "P0";
        }*/
        output.setNome(nomeTeste);

        if (sensoresFluxoNotRotas) {
            if(sensoresMaisRotas)
                nomeTeste = nomeTeste + "_F";
            else
                nomeTeste = nomeTeste + "_Fr";
        } else {
            if(sensoresMaisRotas)
                nomeTeste = nomeTeste + "_Rmais";
            else
                nomeTeste = nomeTeste + "_Rmenos";
        }
        
         if(sensoresCorrelacao)
                nomeTeste = nomeTeste + "C";

        nomeTeste = nomeTeste + "_" + tempoPriori + "t" + tempoProblema;

        System.out.println("PROC: Preparando para iniciar " + nomeTeste + "... " + horaAtual());

        
        tdd = new Double[runs];
        re = new Double[runs];
        mae = new Double[runs];
        rmse = new Double[runs];
        fitness = new Double[runs];
        maeIn = new Double[runs];
        gehIn = new Double[runs];
        geh = new Double[runs];
        percGeh = new Double[runs];
        r2links = new Double[runs];
        r2odm = new Double[runs];
        rmseIn = new Double[runs];
        fitnessIn = new Double[runs];

        this.virtualSensors = virtualSensors1;
        this.ODmatrix = ODmatrix1;
        ODmatrix.calcMatrixClust();
        ODmatrix.calcVarianciaODMatrix();
        //ODmatrix.normalizarPathLinkMatrix(); - ja normalizado na criação
        //ODmatrix.printStatsVariancia(tempoProblema);
        //ODmatrix.redefinirODparArestaContAPartirDeVirtualSensors(virtualSensors, tempoProblema, batchProblema);
        //this.map = map1;

        virtualSensors.calcVarianciaArestas();
        // virtualSensors.printStatsVariancia(tempoProblema, arestasSensor);
        this.tempoPriori = tempoPriori;
        this.batchPriori = batchPriori;
        this.tempoProblema = tempoProblema;
        this.batchProblema = batchProblema;
        this.arestasSensor = arestasSensor;
        this.clusters = clusters;

        N = clusters * clusters;
        parODcoberto = new boolean[clusters * clusters];
        for (int c = 0; c < clusters * clusters; c++) {
            parODcoberto[c] = false;
        }

       
        definirArestas_NSLP();
        
        descobrirODparIndArestaMaisMov();

        posPart = new double[pop * 2][N];
        localBest = new double[pop * 2][N];

        if (!probArestaODdefinidas) {
            this.definirVariaveisProbabilidadeArestaRota(clusters);
            virtualSensors = encontrarMinMaxPorTipoAresta(virtualSensors);
            posPart = new double[pop * 2][N + doParOD.size()];
            localBest = new double[pop * 2][N + doParOD.size()];
        } else {
            definirAssignmentMatrix2(tempoPriori);
        }

        DescriptiveStatistics ds = new DescriptiveStatistics();
        for (int o = 0; o < clusters; o++) {
            for (int d = 0; d < clusters; d++) //para todos os pares OD
            {
                for (int ars = 0; ars < ODparIndArestaMaisMovENCONTRADOS[o][d].size(); ars++) {
                    ds.addValue(ODmatrix.getODParArestaCont(o, d, tempoPriori, ODparIndArestaMaisMovENCONTRADOS[o][d].get(ars)));
                }
            }
        }
        //printStats(ds, "Fatores arestas OD");

        String tempo = horaAtual().replace(",", "") + ", " + runs + ", ";
        String resumo = "s=" + this.pop + ", t=" + this.MAX_GERACOES + ", OD=" + N + ", sensores=" + arestasSensor + ", ";

        somaSolucoes = 0;

        Double[] evo = new Double[MAX_GERACOES];
        Double[] evo2 = new Double[MAX_GERACOES];
        ArrayList[] data = new ArrayList[runs];
        ArrayList[] data2 = new ArrayList[runs];
        ArrayList<String> labelSeries = new ArrayList<>();
        ArrayList<Double> labelValues = new ArrayList<>();

        System.out.println("GA: " + runs + " rodadas; 	Mutação = " + MUTATION_PROBABILITY + ".  Crossover = " + CROSSOVER_PROBABILITY + ";     População = " + pop + ";    Gerações = " + MAX_GERACOES + ".");
        //System.out.println("// TAGC // CGTA  TGCA // TCAG || ACTG  CTGA //  AGCT // GCTA  //   GACT // ~ ");
//inicia contagem de tempo

        minInicio = getMinutosAtual();
        int inicio = 0;

        if (salvandoBackup && !testeParametros) {

            inicio = carregarDadosAnteriores(runs);

        }

        double maeI, rmseI, gehI, pgI, fI;

        for (rodada = inicio; rodada < runs; rodada++) { //um novo GA a cada rodada

            labelSeries.add("r" + rodada);
            ultimaMelhoriaGA = 0;
            Fitness = new double[pop * 2];
            ordem = new int[pop * 2];
            pai = new int[pop / 2];
            maeGA = new int[pop / 2];
            melhorFitness = 0;

            //Gera população
            iniciaParts(min, max, false);
            CalcAllFitness();
            globalBest = ordem[0];

            maeI = calcMAE();//maeIn[rodada - 1] = calcMAE();
            rmseI = calcRMSE();//rmseIn[rodada - 1] = calcRMSE();
            gehI = calcGEH(false);//gehIn[rodada - 1] = this.calcGEH(false);
            fI = melhorFitness; //fitnessIn[rodada - 1] = melhorFitness;

            System.out.println("RUN " + nomeTeste + " " + (rodada+1) + "/" + runs + ": Melhor Fitness inicial é " + melhorFitness + "(RMSE: " + rmseI + "; MAE: " + maeI + "; GEH = " + gehI + ") " + horaAtual());

            for (int g = 0; g < MAX_GERACOES; g++) {  //Laço principal

                globalBest = ordem[0];
                calculaCusto(ordem[0], true, false);
                evo[g] = gehGlobal;//calcGEH(false);//melhorFitness;
                evo2[g] = melhorFitness;

                escolhaPais(g);  //Usa função GARoleta para criar casais de pais. Pode repetir pais
                crossover(g);  //Filhos nascem. Mutação ocorre antes do calculo de fitness

                /*if ((g - ultimaMelhoriaGA) > reset) {
                    iniciaParts(min, max, true);
                    ultimaMelhoriaGA = g;
                }*/
            } //// fim do algoritmo genético, a parte abaixo serve apenas para estatisticas

            globalBest = ordem[0];
            System.arraycopy(posPart[globalBest], 0, localBest[globalBest], 0, localBest[globalBest].length);

            if (salvandoBackup && !testeParametros) {

                rodada = carregarDadosAnteriores(runs);

            }

            if (rodada <= runs) {
                
                tdd[rodada] = this.calcTDDodm();
                re[rodada] = this.calcREodm();
                maeIn[rodada] = maeI;
                rmseIn[rodada] = rmseI;
                gehIn[rodada] = gehI;
                fitnessIn[rodada] = fI;
                data[rodada] = vetorToArray(evo);
                data2[rodada] = vetorToArray(evo2);
                mae[rodada] = calcMAE();
                rmse[rodada] = calcRMSE();
                geh[rodada] = calcGEH(rodada==3);
                percGeh[rodada] = this.percGehAbaixo5;
                fitness[rodada] = melhorFitness;
                r2links[rodada] = r2Global;
                r2odm[rodada] = calcR2Odm();
                results.add(geh[rodada]); // para o iRACE
            }else{
                System.out.println("WARNING: Resultados descartados. Vetores de resultados ja estao completos.");
                ArrayList<Double> aviso = new ArrayList<>();
                aviso.add(666.0);
                return aviso;
            }

            if (rodada == 0) {
                fitnessBestMod = melhorFitness;
                bestMOD = localBest[globalBest];
            } else if (melhorFitness > fitnessBestMod) {
                fitnessBestMod = melhorFitness;
                bestMOD = localBest[globalBest];
            }

            if (salvandoBackup && rodada != (runs) && !testeParametros) {
                //algs = new resultsAlgoritmos();
                //algs = algs.recuperarArquivo();
                algs.addPartialResults(nomeTeste, arestasSensor, rmse, mae, fitness, geh,
                        rmseIn, maeIn, fitnessIn, gehIn, bestMOD, tempo, (rodada), r2links, r2odm, percGeh, tdd, re);
                algs.salvarArquivo(false);
            }

            System.out.println("RUN " + nomeTeste + " " + (rodada+1) + "/" + runs + ": Melhor  fitness  final é " + melhorFitness + printMetricasFinais(rodada) + "\n");
            output.addText(horaAtual().replace(",", "") + "> RUN " + nomeTeste + " " + (rodada+1) + "/" + runs + printMetricasFinais(rodada));

        }

        tempo = tempo + horaAtual().replace(",", "") + ", " + minInicio;
        
        resultsAlgoritmos rest = new resultsAlgoritmos();
        rest = rest.recuperarArquivo();
        rest.addResultados(nomeTeste, arestasSensor, rmse, mae, fitness, geh,
                rmseIn, maeIn, fitnessIn, gehIn, localBest[globalBest], tempo + ", " + resumo, r2links, r2odm, percGeh, tdd, re);
        System.out.println("ALERT: Salvando a ultima MOD resposta. Não a melhor.");
        rest.salvarArquivo(true);
        
        if(!salvandoBackup){
        for (int a = 0; a < MAX_GERACOES; a++) {
            labelValues.add(a * 1.0);
        }
        /*GeraGraficos gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha("Graficos", "evolution GA GEH", data, labelValues, labelSeries, "Iterations (p=" + pop + ";g=" + MAX_GERACOES + ")", "Fitness (" + nomeTeste + ")");
        gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha("Graficos", "evolution GA F", data2, labelValues, labelSeries, "Iterations (p=" + pop + ";g=" + MAX_GERACOES + ")", "Fitness (" + nomeTeste + ")");*/

        }
        
        minInicio = (((double) getMinutosAtual()) - minInicio) / ((double) runs);
        tempo = tempo + horaAtual().replace(",", "") + ", " + minInicio;
//fim contagem de tempo

        if (!testeRedeFechada) {

            double[] respMOD = new double[N];
            for (int o = 0; o < clusters; o++) {
                for (int d = 0; d < clusters; d++) {
                    respMOD[o * clusters + d] = ODmatrix.getODMatrixClustersBatch(o, d, tempoProblema, batchProblema);
                }
            }
            Double vet[] = new Double[1];
            vet[0] = 0.0;
            //rest.addResultados("MOD_t" + tempoProblema + "b" + batchProblema, 0, vet, vet, vet, vet, vet, vet, vet, vet, respMOD, "", vet, vet, vet);
            rest.salvarArquivo(true);   //gera boxplot ao salvar arquivo
        } else {

            rest.salvarArquivo(true); //salvar resultados do GA    
            geradordeRede2 ger = new geradordeRede2();
            //somente para salvar arquivo de resultados
        }

        output.addText(horaAtual() + ": OK!! " + nomeTeste + " CONCLUIDO!");
        output.setConcluido();

        return results;
    }

    public void printSolution() {
        String z = "";

        if (ordem.length > 0) {
            for (int a = 0; a < posPart[ordem[0]].length; a++) {
                z = z + posPart[ordem[0]][a] + " ";
            }
        } else {
            for (int a = 0; a < localBest[globalBest].length; a++) {
                z = z + localBest[globalBest][a] + " ";
            }
        }

        System.out.println(z + "\n");
    }

    public void CalcAllFitness() {

        int individuo;

        for (individuo = 0; individuo <= (pop * 2 - 1); individuo++) {
            ordem[individuo] = individuo;
            Fitness[ordem[individuo]] = calculaCustoGA(ordem[individuo], 0);
            //   trocaEsquerda(individuo);
        }
        bubble(0, pop * 2);
        //quickSort();
    }

    public void trocaEsquerda(int esc) {
        int i;

        if (esc > 0) {
            if (Fitness[ordem[esc]] > Fitness[ordem[esc - 1]]) {

                i = ordem[esc];
                ordem[esc] = ordem[esc - 1];
                ordem[esc - 1] = i;

                trocaEsquerda(esc - 1); //recursão. Continua com mesmo individuo, basicamente
            }
        } else {
            globalBest = ordem[0];
            System.arraycopy(posPart[globalBest], 0, localBest[globalBest], 0, N);

        }

    }

    public void bubble(int inicio, int fim) {
        int i;

        for (int esc = inicio; esc < fim; esc++) {
            if (esc > 0) {
                if (Fitness[ordem[esc]] > Fitness[ordem[esc - 1]]) {

                    i = ordem[esc];
                    ordem[esc] = ordem[esc - 1];
                    ordem[esc - 1] = i;

                    esc = esc - 2;//volta 2 atrás, avança um pelo for, está testando se o valor que acabou de mudar de posição vai mudar outra posição
                }
            }
        }
        //fim da ordenação
        globalBest = ordem[0];
        melhorFitness = Fitness[ordem[0]];
        System.arraycopy(posPart[globalBest], 0, localBest[globalBest], 0, N);
    }

    public void quickSort() {

        ordem = quick.getNewORDEM(Fitness);
        globalBest = ordem[0];
        melhorFitness = Fitness[ordem[0]];
        System.arraycopy(posPart[globalBest], 0, localBest[globalBest], 0, N);

    }

    public void Mutate(int mutante, boolean forceMutate) {

        /* 
        int aux1, aux2;
        int cont = 0;
        while (Math.random() < MUTATION_PROBABILITY  && cont<10) {

            boolean antes = Math.random() > 0.5;

            aux1 = (int) (Math.random() * (N));
            aux2 = (int) (Math.random() * (N));

            if (parODcoberto[aux1]) {
                if (antes) {
                    posPart[mutante][aux1] = posPart[mutante][aux1] * (1 + 0.2*Math.random()); ///  - 6 + (4 * Math.random());
                } else {
                    posPart[mutante][aux1] = posPart[mutante][aux1] * (1 - 0.2*Math.random()); ///  - 6 + (4 * Math.random());  + 6 - (4 * Math.random());
                }
                if (posPart[mutante][aux1] < 0) {
                    posPart[mutante][aux1] = 0;
                }
            }
            if (parODcoberto[aux2]) {
                if (antes) {
                    posPart[mutante][aux1] = posPart[mutante][aux1] * (1 - 0.2*Math.random()); ///  - 6 + (4 * Math.random());
                } else {
                    posPart[mutante][aux1] = posPart[mutante][aux1] * (1 + 0.2*Math.random()); ///  - 6 + (4 * Math.random());  + 6 - (4 * Math.random());
                }
                if (posPart[mutante][aux2] < 0) {
                    posPart[mutante][aux2] = 0;
                }
            }
            cont++;
        } */
        if (Math.random() < MUTATION_PROBABILITY || forceMutate) {
            for (int aux = 0; aux < N; aux++) {
                if (parODcoberto[aux]) {
                    if (Math.random() < MUTATION_PROBABILITY) {
                        posPart[mutante][aux]
                                = posPart[mutante][aux]
                                + posPart[mutante][aux] * (varMutacao - 2 * Math.random() * varMutacao);        //((varMutacao)*2*Math.random());

                        if (posPart[mutante][aux] < 0) {
                            posPart[mutante][aux] = 0;
                        }

                    }
                }
            }
        }

    }

    public void crossover(int g) {

        int crossPoint1, crossPoint2, casal, xx;
        int numeroFilhos = 0;
        boolean forceMutate;

        for (casal = 0; casal <= ((pop / 2) - 1); casal++) {

            if (Math.random() > CROSSOVER_PROBABILITY) {

                forceMutate = false;
                crossPoint1 = (int) Roleta(1, N / 2);
                crossPoint2 = (int) Roleta(crossPoint1 + 1, N - 1);
                //System.out.println("Crossover(0,"+crossPoint1+","+crossPoint2+","+N+") pop="+pop+";");

                for (xx = 0; xx < crossPoint1; xx++) { //gera primeira parte dos dois filhos                                             	 

                    posPart[ordem[pop + numeroFilhos]][xx] = posPart[ordem[pai[casal]]][xx];
                    posPart[ordem[pop + numeroFilhos + 1]][xx] = posPart[ordem[maeGA[casal]]][xx];

                }

                for (xx = crossPoint1; xx < crossPoint2; xx++) {//gera segunda parte dos dois filhos                                             	 

                    posPart[ordem[pop + numeroFilhos]][xx] = posPart[ordem[maeGA[casal]]][xx];
                    posPart[ordem[pop + numeroFilhos + 1]][xx] = posPart[ordem[pai[casal]]][xx];

                }

                for (xx = crossPoint2; xx < N; xx++) {  //gera terceira parte dos dois filhos                                             	 

                    posPart[ordem[pop + numeroFilhos]][xx] = posPart[ordem[pai[casal]]][xx];
                    posPart[ordem[pop + numeroFilhos + 1]][xx] = posPart[ordem[maeGA[casal]]][xx];

                }

            } else {
                forceMutate = true;
                for (xx = 0; xx < N; xx++) {  //não houve crossover

                    posPart[ordem[pop + numeroFilhos]][xx] = posPart[ordem[pai[casal]]][xx];
                    posPart[ordem[pop + numeroFilhos + 1]][xx] = posPart[ordem[maeGA[casal]]][xx];

                }

            }

            for (int z = 0; z < 2; z++) {

                Mutate(ordem[pop + numeroFilhos + z], forceMutate);
                Fitness[ordem[pop + numeroFilhos + z]] = calculaCustoGA(ordem[pop + numeroFilhos + z], g);
                if (casaisDinamicos) {
                    trocaEsquerda(pop + numeroFilhos + z); //ordena
                }
            }

            /*Mutate(ordem[pop + numeroFilhos]);
            Fitness[ordem[pop + numeroFilhos]] = calculaCustoGA(ordem[pop + numeroFilhos]);
            if (casaisDinamicos == 1) {
                trocaEsquerda(pop + numeroFilhos); //ordena
            }
            Mutate(ordem[pop + numeroFilhos + 1]);
            Fitness[ordem[pop + numeroFilhos + 1]] = calculaCustoGA(ordem[pop + numeroFilhos + 1]);
            if (casaisDinamicos == 1) {
                trocaEsquerda(pop + numeroFilhos + 1);
            }*/
            numeroFilhos = numeroFilhos + 2;

        }//Next casal

//acabou geraçao
        if (!casaisDinamicos) {

            bubble(0, pop * 2);
            //quickSort());     
            /*for (casal = 0; casal < pop*2; casal++) { //apenas aproveitando a variavel
                trocaEsquerda(casal); //ordena dos primeiros filhos antes
            }*/

        }

    }

    public static void printStats(DescriptiveStatistics d, String name) {
        DecimalFormat df2 = new DecimalFormat(".######");
        String t = "STATS: " + name + ": Min. " + df2.format(d.getMin()) + "; Mean. " + df2.format(d.getMean()) + "; Max. " + df2.format(d.getMax()) + "; ";

        for (int p = 5; p < 100; p = p + 15) {
            t = t + "p" + p + " " + df2.format(d.getPercentile(p)) + "; ";
        }

        System.out.println(t);
    }

    /**
     * @param output the output to set
     */
    public void setOutput(Output output) {
        this.output = output;
        output.setIconImage(getIcone().getImage());
    }

    /**
     * @param testeRedeFechada the testeRedeFechada to set
     */
    public void setTesteRedeFechada(boolean testeRedeFhada, Mapping m) {
        this.testeRedeFechada = testeRedeFhada;
        if(testeRedeFhada)
            mape = m;
    }

    public double calcR2Odm() {

        double[] odm = new double[N];

        for (int a = 0; a < clusters; a++) {
            for (int b = 0; b < clusters; b++) {
                odm[a * clusters + b] = ODmatrix.getODMatrixClustersBatch(a, b, tempoProblema, batchProblema);
            }
        }

        return calcR2(localBest[globalBest], odm);
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

    /**
     * @param varMutacao the varMutacao to set
     */
    public void setVarMutacao(double varMutacao) {
        this.varMutacao = varMutacao;
    }

    /**
     * @param sensoresFluxoNotRotas sensores por fluxo
     * @param sensoresMaisRotas sensores mais rotas
     * @param correlacaoSens usa correlacao
     * @param OSMFileLocation2 local do arquivo map
     * to set
     */
    public void setSensoresFluxoNotRotas(boolean sensoresFluxoNotRotas, boolean sensoresMaisRotas, boolean correlacaoSens, String OSMFileLocation2) {
        this.sensoresFluxoNotRotas = sensoresFluxoNotRotas;
        this.sensoresMaisRotas = sensoresMaisRotas;
        this.sensoresCorrelacao = correlacaoSens;
        OSMFileLocation = OSMFileLocation2;
    }

    public void setMinMaxRand(int min1, int max1) {
        min = min1;
        max = max1;
    }

    /**
     * @param max the max to set
     */
    public void setMax(int max) {
        this.max = max;
    }

    /**
     * @param min the min to set
     */
    public void setMin(int min) {
        this.min = min;
    }

    //Codigo super importante, só que não.
    private ImageIcon icone = null;

    public ImageIcon createImageIcon(String path,
            String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public ImageIcon getIcone() {

        if (icone == null) {
            icone = createImageIcon("genius.jpg", "processing");
        }

        return icone;

    }

    /**
     * @param salvandoBackup the salvandoBackup to set
     */
    public void setSalvandoBackup(boolean salvandoBackup) {
        this.salvandoBackup = salvandoBackup;
    }

    /**
     * @return the funcaoFitness
     */
    public String getFuncaoFitness() {
        return funcaoFitness;
    }

    /**
     * @param funcaoFitness the funcaoFitness to set
     */
    public void setFuncaoFitness(String funcaoFitness) {
        this.funcaoFitness = funcaoFitness;
    }

    /**
     * @param mape the mape to set
     */
    public void setMape(Mapping mape) {
        this.mape = mape;
    }

    
    
    // definição NSLP
    
    //int [] sensorAresta = virtualSensors.getArestasMaismovimentadasIndexARESTA();
    
    
        private boolean lerArquivoOSMDat() {

        if (mape != null) {
            return true;
        }

        try {

            ObjectInputStream objectIn
                    = new ObjectInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(OSMFileLocation + ".dat")));
            mape = (Mapping) objectIn.readObject();

            objectIn.close();

            System.out.println("OK: Recuperou objeto mapa de '" + OSMFileLocation + ".dat'!");
            return true;
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("ALERT: Não recuperou registros de arquivo '" + OSMFileLocation + ".dat'");
            return false;
        }
    }
    
    
    
}
