/* */
package taxi.od.solver;

import auxs.GeraGraficos;
import auxs.GeradorRotaSUMO;
import auxs.Output;
import auxs.PSO;
import auxs.geradordeRede2;
import auxs.resultsAlgoritmos;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * @author Luciano
 */
public class TaxiODSolver {

    static String version = "7.3 - 18.05.11";

    ////************  COMPORTAMENTO
    static boolean rodarAlgoritmosEstimativaOD = true;
    static int numSensores;
    static int runs = 30;
    static boolean useMatrixPriori = false;

    static boolean processarSensoresVirtuais = false;
    static boolean utilizarKmeans = false; //se falso, recupera cluster de arquivo do WEKA

    static boolean gerarResumoDados = false;
    static boolean gerarDatasetsDerivados = false;
    static boolean selecionarRegistrosParaDatasetsDif = false;

    static boolean showDetalheViagens = false;

    //**************************** PARAMETROS
    static int linesToRead = 285000;//285.000 171060; //total de porto: 1.710.671
    static int taxiBatches = 1; //34
    static int profundidadeBuscaCompletarCaminhos = 20;

    static int metrosRaioNode = 10;
    static boolean descobrirLimitesTaxisTrips = false;
    static boolean calcWaysPorNode = false;
    static double minMinCorrida = 3; //minimo de minutos de corrida
    static int minimoNodes = 3;
    static boolean mantemComMissing_data = false;
    static int registrosPorMinuto = 4;

    static int ROIsizeM = 1000;
    static int discretTemporal = 24;
    static int tempoInicio = 7;
    static int tempoFim = 17; //17
    static int tempoInicio2 = 20;
    static int tempoFim2 = 20;

    static int numeroClusters = 21;
    // static final double bordaClusterM = 40.0;
    static boolean todosDetalhesBlocosCsvODmatrix = false;
    static int minimoContParaMostrarLinhaColunaBlocos = 20;
    static double percentTrafegoToShowBordas = 0.92;

    static double metrosMaxParaSerNodeGemeo = 2.8;
    static boolean usarNodesGemeosComoVizinhos = false;

    static boolean weekDays = true;
    static boolean weekEnd = false;

    // FIM PARAMETROS ***************************
    static String TaxiFileLocation = "porto data.csv";
    static String OSMFileLocation = "porto map.xml";
    static String ODMatrixFileLocation = "porto odmatrix";
    static String VirtualSensorsFile = "porto virt sen";

    static String pastaGraficos = "RES graficos";
    static String pastaPlanilhasResumos = "RES planilhas";

    static taxi_Trip_Instance[] viagens_Taxi;
    static int cont_viagens_Taxi;
    static ODMatrix ODmatrix;
    static Mapping mapa;
    static VirtualSensors virtualSensors;

    static boolean novoOSMDat = false;
    static boolean usandoOSMData = true;
    static boolean usandoTrafficCounts = false;

    static int minRand = 0;
    static int maxRand = 800;

    static final DecimalFormat df2 = new DecimalFormat(".##");


    public static void main(String args[]) {

        System.out.println("Taxi OD Solver - Versão " + version + "  - Inicio em " + horaAtual() + "\n");

        //testarDiferentesMapMatching();
        escolherDadosResultsAlgs(1);
    
        //resultsAlgoritmos alg = new resultsAlgoritmos();
        //alg = alg.recuperarArquivo();
        //alg.joinWithFile("resultsAlgs2.dat");
        //alg.salvarArquivo(true);
        //GeradorRotaSUMO sumonador = new GeradorRotaSUMO(19, 10, 2017, 8.0, 11.0); return; //26,2

        
        //********* Casos para comportamento diferenciado
        if (processarSensoresVirtuais) {
            processarVirtSensorsEODMatrixPorBatches();//processarVitualSensorsPorBatches();
        }

        if (gerarDatasetsDerivados) {
            gerarDatasetsDiferenciados();
        }
        if (gerarResumoDados) {
            calcularResumoDados();
        }
        
        //mudarArquivosParaEstudoCasoCuritiba();
        
        
        numSensores = 500;
        runs = 30;
        int fator = 2;
        
        //runODEstimation();
        
        //useMatrixPriori = true;
        
        if (rodarAlgoritmosEstimativaOD) {
            int h = 8;
            int s = numSensores;
            if (!usandoOSMData) {
                h = 0;
                s = 71;
                //criarCODGusek(h, s);
            }
            
            
            
            //paralelizarRodadasGAePSO(h, true,"folga", fator); // Rodadas de PSO paralelizadas
            //paralelizarRodadasGAePSO(h, false,"folga", fator); // Rodadas de GA paralelizadas            
            //paralelizarRodadasGAePSO(h, true,"reg", fator); // Rodadas de PSO paralelizadas
            //paralelizarRodadasGAePSO(h, false,"reg", fator); // Rodadas de GA paralelizadas            
            //paralelizarRodadasGAePSO(h, true,"geh", fator); // Rodadas de PSO paralelizadas
            //paralelizarRodadasGAePSO(h, false,"geh", fator); // Rodadas de GA paralelizadas         
            //runODEstimationResumo(h, s, true, false, false, false, true, true, false, false, fator, "reg");   //Executar ALGEB e digerir MILP  (hora,numS, algeb,runGA,runPSO,gerarCodGUSEK,digerirPL, porFluxos, maisRotas, fator, eqFitness) - 
            
            
            //TESTES EQ FITNESS>     hora, numSensores, algeb, runGA, runPSO, gerarCodGUSEK, digerirPL, porFluxos, maisRotas, correlacSens,fator, eqFitness)
            //runODEstimationResumo(h, s, false, false, true, false, false, false, true, false, fator, "folga");
            //runODEstimationResumo(h, s, false, false, true, false, false, false, true, false, fator, "geh");
            //runODEstimationResumo(h, s, false, false, true, false, false, false, true, false, fator, "reg");
            //runODEstimationResumo(h, s, false, !false, !true, false, false, false, true, false, fator, "folga");
            //runODEstimationResumo(h, s, false, !false, !true, false, false, false, true, false, fator, "geh");
            //runODEstimationResumo(h, s, false, !false, !true, false, false, false, true, false, fator, "reg");
            
            //TESTES ESCOLHA SENSORES> (algeb, runGA,runPSO,gCdGUSEK,digPL, porFx, maisR correlacSens,int fator, String eqFitness)
            //runODEstimationResumo(h, s, false, false, true, false, false, false, true, false, fator, "geh"); //PSO
            //runODEstimationResumo(h, s, false, false, true, false, false, true, true, false, fator, "geh");
            //runODEstimationResumo(h, s, false, false, true, false, false, false, false, false, fator, "geh");
            //runODEstimationResumo(h, s, false, false, true, false, false, true, false, false, fator, "geh");
            //runODEstimationResumo(h, s, false, !false, !true, false, false, false, true, false, fator, "geh"); //GA
            //runODEstimationResumo(h, s, false, !false, !true, false, false, true, true, false, fator, "geh");
            //runODEstimationResumo(h, s, false, !false, !true, false, false, false, false, false, fator, "geh");            
            //runODEstimationResumo(h, s, false, !false, !true, false, false, true, false, false, fator, "geh");
            //runODEstimationResumo(h, s, true, false, !true, false, false, false, true, false, fator, "geh"); //LS
            //runODEstimationResumo(h, s, true, false, !true, false, false, true, true, false, fator, "geh");
            //runODEstimationResumo(h, s, true, false, !true, false, false, false, false, false, fator, "geh");            
            //runODEstimationResumo(h, s, true, false, !true, false, false, true, false, false, fator, "geh");
            //runODEstimationResumo(h, s, false, false, false, false, true, false, true, false, fator, "geh"); //MILP
            //runODEstimationResumo(h, s, false, false, false, false, true, true, true, false, fator, "geh");
            //runODEstimationResumo(h, s, false, false, false, false, true, false, false, false, fator, "geh");            
            //runODEstimationResumo(h, s, false, false, false, false, true, true, false, false, fator, "geh");
            
            
            //runODEstimationNumSensores(8, 200, 100, 499, 2);   // <-- teste número de sensores. PSO, horário, inicio, intervalo e fim.  Fator
            //runODEstimationNumSensores(8, 500, 250, 2002, 2);
            
          
            
            //testarEscolhasRotas(h,true);
            
        }

    }
       
    //parametros PSO e GA (Porto)
    static double c1=0.13, c2=1.0, wIn=0.85, wF=0.25;
    static int s_pso=862, it_pso=3480, it_reset_pso=85;
    static int pop=515, geracoes=5825;
    static double crossover=0.28, mutacao=0.95, varMut=0.01;
    
    public static void mudarArquivosParaEstudoCasoCuritiba() {

        TaxiFileLocation = "cwb.csv"; //Nao chega a usar
        OSMFileLocation = "cwb map.xml"; //Nao usa para ler, mas usa para salvar
        ODMatrixFileLocation = "cwb odmatrix";
        VirtualSensorsFile = "cwb virt sen";
        usandoOSMData = false;
        minRand = 0;
        maxRand = 1500;
        loadOSMData();
        
    //parametros PSO e GA (Curitiba)    
    c1=0.44; c2=0.89; wIn=0.90; wF=0.07;
    s_pso=1404; it_pso=2136; it_reset_pso=71;
    pop=50; geracoes=40000;
    crossover=0.85; mutacao=0.34; varMut=0.1;

    }

    public static void paralelizarRodadasGAePSO(int hora, boolean PSO, String funcFitness, int fatorMult) {

        Output output = new Output();
        System.out.println("OK: Iniciando paralelizacao(hora=" + hora + ", PSO=" + PSO + ")");
        output.setTitle("Loading...");
        output.addText(horaAtual() + ">> Iniciando componentes... ");

        if (!lerArquivoODMatrixDat() || !lerArquivoVirtualSensors(0)) {
            System.out.println("Necessário criar arquivo ODMatrix/VirtualSensors antes.");
            processarVirtSensorsEODMatrixPorBatches();//processarODTripMatrixPorBatches();
        }
        
        PSO pso = new PSO(VirtualSensorsFile, discretTemporal);
        pso.setMinMaxRand(minRand, maxRand);
        pso.setUseMatrixPriori(useMatrixPriori);
        pso.setUseVariance(false);
        pso.setOutput(output);
        pso.setTesteRedeFechada(!usandoOSMData, mapa);
        pso.setFuncaoFitness(funcFitness);
        pso.setSalvandoBackup(true);

        if (PSO) {
            pso.setParamPSO(s_pso * fatorMult, it_pso * fatorMult , wIn, wF, c1, c2, it_reset_pso, 0.9);//pso.setParamPSO(1404 * fatorMult, 862 * fatorMult , 0.9, 0.07, 0.44, 0.89, 85, 0.9);
            //(numP, iterat,  wIn,  wF,  c1, c2)
            pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                    hora, hora, numSensores, runs);
        } else {
            pso.setParamGA(pop * fatorMult, geracoes * fatorMult, crossover, mutacao, varMut); //(int populacao, int geracoes, double crossover, double mutacao, double varMut)
            pso.runGA(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), hora, 0, hora, 0, numSensores, runs);
        }

    }

     public static void testarEscolhasRotas(int hora, boolean PSO, int fatorMult){
         
         if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }
         
         //com correlacao
         testeEscolhasSensores(hora,PSO,true,false,true,fatorMult);
         testeEscolhasSensores(hora,PSO,false,true,true,fatorMult);
         testeEscolhasSensores(hora,PSO,false,false,true,fatorMult);
         //sem correlaçao
         testeEscolhasSensores(hora,PSO,true,false,false,fatorMult);
         testeEscolhasSensores(hora,PSO,false,true,false,fatorMult);
         testeEscolhasSensores(hora,PSO,false,false,false,fatorMult);
     
     }
    
    
       public static void testeEscolhasSensores(int hora, boolean PSO, boolean sensoresFluxo, boolean maisRotas, boolean correlacSens, int fatorMult) {

        Output output = new Output();
        System.out.println("OK: Teste escolha sensores(hora=" + hora + ", PSO=" + PSO + ")");
        output.setTitle("Loading...");
        output.addText(horaAtual() + ">> Iniciando componentes... ");

        if (!lerArquivoODMatrixDat() || !lerArquivoVirtualSensors(0)) {
            System.out.println("Necessário criar arquivo ODMatrix/VirtualSensors antes.");
            processarVirtSensorsEODMatrixPorBatches();//processarODTripMatrixPorBatches();
        }
        
        PSO pso = new PSO(VirtualSensorsFile, discretTemporal);
        pso.setMinMaxRand(minRand, maxRand);
        pso.setUseMatrixPriori(useMatrixPriori);
        pso.setUseVariance(false);
        pso.setOutput(output);
        pso.setTesteRedeFechada(!usandoOSMData, mapa);
        //pso.setSalvandoBackup(true);
        pso.setSensoresFluxoNotRotas(sensoresFluxo, maisRotas, correlacSens,OSMFileLocation);

        if (PSO) {
            pso.setParamPSO(s_pso * fatorMult, it_pso * fatorMult , wIn, wF, c1, c2, it_reset_pso, 0.9);
            //(numP, iterat,  wIn,  wF,  c1, c2)
            pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                    hora, hora, numSensores, runs);
        } else {
            pso.setParamGA(pop * fatorMult, geracoes * fatorMult, crossover, mutacao, varMut); //(int populacao, int geracoes, double crossover, double mutacao, double varMut)
            pso.runGA(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), hora, 0, hora, 0, numSensores, runs);
        }
        
        pso.outputDispose();
    }
    
    
    
    public static void mudarArquivosParaNewYork() {

        TaxiFileLocation = "nyc.csv"; //Nao chega a usar
        OSMFileLocation = "G:\\nyc Large.osm"; //Nao usa para ler, mas usa para salvar
        ODMatrixFileLocation = "nyc odmatrix";
        VirtualSensorsFile = "nyc counts.csv";
        usandoTrafficCounts = true;
        //usandoOSMData = false;
    }

    public static void escolherDadosResultsAlgs(int repeat) {

        resultsAlgoritmos res = new resultsAlgoritmos();
        res = res.recuperarArquivo();
        int max = res.printDadosDisponiveis();
        Scanner scanner = new Scanner(System.in);
        int ent = 0;
        ArrayList<Integer> escolhidos = new ArrayList<>();
        System.out.println("INF: Digite valores entre " + 0 + " e " + max + ". (-1 para salvar, -6 para quit)");
        while (ent >= 0) {
            ent = scanner.nextInt();
            if (ent >= 0 && ent < max && escolhidos.indexOf(ent) == -1) {
                escolhidos.add(ent);
                System.out.println("Adicionou opção " + ent + ": " + res.getNomeDados().get(ent));
            } else if (ent >= max) {
                System.out.println("Opção inválida.");
            } else if(escolhidos.indexOf(ent) != -1){
                System.out.println("Opção já adicionada anteriormente.");
            }
        }

        if (ent == -6) {
            System.out.println("QUIT: escolherDadosResultsAlgs");
            return;
        }

        System.out.println("OK: Números escolhidos. Iniciando relatório reduzido!");

        res.criarRelatorioReduzido(escolhidos);

        if (repeat > 0) {
            System.out.println("INF: Mais " + repeat + " repetição de escolhas.");
            escolherDadosResultsAlgs(repeat - 1);
        }

    }

    public static void testeSeGrafoEhConexo() {

        System.out.println("PROC: Iniciando teste de conectividade de grafo");

        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }

        if (!lerArquivoVirtualSensors(0)) {
            return;
        }

        boolean[] conectado = new boolean[mapa.getContNodes()];
        for (int n = 0; n < mapa.getContNodes(); n++) {
            conectado[n] = false;
        }

        ArrayList<Integer> viz;
        conectado[0] = true;

        for (int t = 0; t < 20000; t++) {
            for (int n = 0; n < mapa.getContNodes(); n++) {

                if (conectado[n]) {

                    viz = mapa.getVizinhosIndexDoNode(n, true);
                    for (int v = 0; v < viz.size(); v++) {
                        conectado[viz.get(v)] = true;
                    }
                }

            }
            if (t % 10000 == 0) {
                System.out.println("Passada " + t + " ok...");
            }
        }

        String r = "Conexo!";
        int err = 0;

        String cod = "http://www.darrinward.com/lat-long/\n\n";

        for (int n = 0; n < mapa.getContNodes(); n++) {
            if (conectado[n] == false) {
                err++;
                r = "desconexo por " + err + "/" + mapa.getContNodes() + " nos!";
                cod = cod + mapa.getNode_lat()[n] + ", " + mapa.getNode_lon()[n] + "\n";
            }
        }

        salvarTxt("nos desconexos.txt", cod);

        System.out.println("OK: Grafo é " + r);

    }

    /*public static void processarODTripMatrixPorBatches() {
        
  
        if(usandoOSMData){
        System.out.println("PROC: Processamento de O-D Matrix por batches: " + linesToRead + " (x " + taxiBatches + " = " + taxiBatches * linesToRead + ") " + horaAtual());

        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }
        // calcularStatsDistanciaNodesMap(mapa);
        mapa.criarROIs();
        novoOSMDat = true;

        if (!lerArquivoVirtualSensors()) {
            System.out.println("Necessário criar arquivo virtualSensors antes.");
            processarVitualSensorsPorBatches();
        }

        createODMatrix();

        if (!utilizarKmeans) {
            ODmatrix.loadDataset5RedefinirClusters("dataset5.arff");
            ODmatrix.resetContagensBlocos(taxiBatches, ODmatrix.getNumeroClusters());
        }

        for (int bat = 0; bat < taxiBatches; bat++) {

            loadTaxiData(bat * linesToRead, linesToRead); //caso não leia, inicia processamento para gerar novo arquivo dat
            mapTaxiTrips(true, false,9.0,1.5,30.0);
            reconstruirCaminhosMap();
            identificarQuadrantesOD(); //Calculos nos objetos de viagem_Taxi
            acrescentarDadosDeTaxiNaODMatrix(bat);

            System.out.println("OK: Fim batch " + (bat + 1) + "/" + taxiBatches + "! " + horaAtual() + "\n");
        }

        ODmatrix.normalizarPathLinkMatrix(discretTemporal);

        if (utilizarKmeans) {
            ODmatrix.criarClusters();
        } else {
            ODmatrix.calcMatrixClust();
        }

        ODmatrix.encontrarNosBordaClusters(mapa);

       // if (descobrirLimitesTaxisTrips) {
       //     descobrirLimites();
       //     summaryViagens(); //calculos em viagens_Taxi      
       // }
        //System.out.println();
        
        }else{ // Nao esta usando OSMData. Hardcoded, geradorDeRede
        
        System.out.println("PROC: Processamento de O-D Matrix. Harcoded, estudo de caso  " + horaAtual());
        geradorDeRede gerador = new geradorDeRede();

        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }
        
        mapa.criarROIs();
        novoOSMDat = true;

        if (!lerArquivoVirtualSensors()) {
            System.out.println("Necessário criar arquivo virtualSensors antes.");
            processarVitualSensorsPorBatches();
        }

        createODMatrix();

        ODmatrix.setODMatrixHardCode(gerador.getMatrizPriori(), gerador.getNumPontosOD());
        ODmatrix.definirCaminhosRedeFechada(mapa, virtualSensors, gerador.getNumPontosOD());       
 
        }
      
        saveFiles(false, true, false);

        //ODmatrix.gerarScriptRgoogleMaps(virtualSensors, mapa, percentTrafegoToShowBordas);

    }*/
    public static void processarRelacaoEntreArestas() {

        boolean novoMap = false;

        System.out.println("PROC: Processamento de Relação entre arestas: " + linesToRead + " (x " + taxiBatches + " = " + taxiBatches * linesToRead + ") " + horaAtual());

        // Converter e indexar localização de vias OSM
        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
            novoMap = true;
        }

        if (!lerArquivoVirtualSensors(0)) {
            System.out.println("ERROR: Precisa de objeto virtualSensors, por ele ter as arestas (processarRelacaoEntreArestas)");
            processarVirtSensorsEODMatrixPorBatches();
        }

        int reserve = taxiBatches;
        taxiBatches = 1;

        ArrayList<Integer> from = virtualSensors.getFromNodIndex();
        ArrayList<Integer> to = virtualSensors.getToNodIndex();

        ArrayList<Integer>[] arestasRelac = new ArrayList[from.size()];
        ArrayList<Double>[] contArestasRelac = new ArrayList[from.size()];

        for (int z = 0; z < from.size(); z++) {
            arestasRelac[z] = new ArrayList<>();
            contArestasRelac[z] = new ArrayList<>();
        }

        int[] contAresta = new int[from.size()];
        ArrayList<Integer> nods;
        ArrayList<Integer> indexArestasDaViagem;
        int arestaAtual;

        for (int bat = 0; bat < taxiBatches; bat++) {
            //carregar dados de taxi
            loadTaxiData(bat * linesToRead, linesToRead); //caso não leia, inicia processamento para gerar novo arquivo dat
            mapTaxiTrips(true, true, 7.0, 2.0, 30.0, 5);
            reconstruirCaminhosMap();
            //repassarViagensParaSensores(bat);

            for (int v = 0; v < viagens_Taxi.length - 1; v++) {
                nods = viagens_Taxi[v].getPosNodesEmMapa(); //pega sequencia de nodes
                indexArestasDaViagem = new ArrayList<>();

                if (nods.size() > 0) {
                    for (int n = 0; n < nods.size() - 1; n++) {
                        //encontra aresta atual
                        arestaAtual = -1;
                        for (int a = 0; a < to.size(); a++) {
                            if (Objects.equals(from.get(a), nods.get(n)) && Objects.equals(to.get(a), nods.get(n + 1))) {
                                arestaAtual = a;
                            }
                        }
                        if (arestaAtual != -1 && indexArestasDaViagem.indexOf(arestaAtual) == -1/*indexArestasDaViagem.indexOf(arestaAtual)!=indexArestasDaViagem.lastIndexOf(arestaAtual)*/) {
                            indexArestasDaViagem.add(arestaAtual);
                        }
                        // System.out.println("Add aresta "+arestaAtual);
                    } // encontrou arestas da viagem
                }

                int arestaProc;

                for (int i = 0; i < indexArestasDaViagem.size(); i++) {

                    arestaProc = indexArestasDaViagem.get(i);
                    contAresta[arestaProc]++;

                    for (int i2 = 0; i2 < indexArestasDaViagem.size(); i2++) {
                        if (i != -i2) {
                            int indz = arestasRelac[arestaProc].indexOf(indexArestasDaViagem.get(i2));
                            if (indz == -1) {
                                arestasRelac[arestaProc].add(indexArestasDaViagem.get(i2));
                                contArestasRelac[arestaProc].add(1.0);
                                // System.out.println("Novo relacion em  "+arestaProc);
                            } else {

                                contArestasRelac[arestaProc]
                                        .set(indz, contArestasRelac[arestaProc].get(indz) + 1.0);
                                //   System.out.println("Relacion crescendo em "+arestaProc);
                            }

                        }
                    }
                }

            }

            System.out.println("OK: Fim batch " + (bat + 1) + "/" + taxiBatches + "! " + horaAtual() + "\n");
        }

        DescriptiveStatistics d = new DescriptiveStatistics();
        DescriptiveStatistics d2 = new DescriptiveStatistics();
        DescriptiveStatistics d3 = new DescriptiveStatistics();
        DescriptiveStatistics d4 = new DescriptiveStatistics();
        DescriptiveStatistics d5 = new DescriptiveStatistics();
        DescriptiveStatistics d6 = new DescriptiveStatistics();
        DescriptiveStatistics d7 = new DescriptiveStatistics();

        //normalizar
        int c1, c2, c3, c4, c6, c7;
        int[] contAgregadorSuper = new int[from.size()];

        for (int a = 0; a < to.size(); a++) { //passando por arestas
            c1 = 0;
            c2 = 0;
            c3 = 0;
            c4 = 0;

            c6 = 0;
            c7 = 0;

            if (contAresta[a] > 0) {

                for (int r = 0; r < arestasRelac[a].size(); r++) { //passando pelas arestas relacionadas

                    // System.out.println("contArestasRelac["+a+"].get("+r+")/contAresta["+a+"] = "+contArestasRelac[a].get(r)+" / "+contAresta[a]+ "  = "+(contArestasRelac[a].get(r) / ((double) contAresta[a])));
                    contArestasRelac[a].set(r, contArestasRelac[a].get(r) / ((double) contAresta[a]));

                    if (contAresta[a] > 2 && contArestasRelac[a].get(r) > 0.9) {
                        contAgregadorSuper[a]++;
                    }

                    if (contArestasRelac[a].get(r) > 1) {
                        contArestasRelac[a].set(r, 1.0);
                        //System.out.println("contArestasRelac["+a+"].get("+r+")/contAresta["+a+"] = "+contArestasRelac[a].get(r)+" / "+contAresta[a]+ "  = "+(contArestasRelac[a].get(r) / ((double) contAresta[a])));
                    }

                    //contagem
                    if (contArestasRelac[a].get(r) > 0.3) {
                        c7++;

                        if (contArestasRelac[a].get(r) > 0.4) {
                            c6++;

                            if (contArestasRelac[a].get(r) > 0.6) {
                                c4++;

                                if (contArestasRelac[a].get(r) > 0.8) {
                                    c3++;

                                    if (contArestasRelac[a].get(r) > 0.9) {
                                        c2++;
                                        if (contArestasRelac[a].get(r) > 0.95) {
                                            c1++;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // d8.addValue(c8);
                    //d9.addValue(c9);
                } //fim if aresta vazia

                d.addValue(c1);
                d2.addValue(c2);
                d3.addValue(c3);
                d4.addValue(c4);
                d5.addValue(arestasRelac[a].size());
                d6.addValue(c6);
                d7.addValue(c7);
            }

        }

        System.out.println("OK: Preencheu objetos DescriptiveStats. Iniciando printStats() " + horaAtual());

        printStats(d, "Arestas Relat >0.95");
        printStats(d2, "Arestas Relat >0.9");
        printStats(d3, "Arestas Relat >0.8");
        printStats(d4, "Arestas Relat >0.6");
        printStats(d6, "Arestas Relat >0.4");
        printStats(d7, "Arestas Relat >0.3");
        //printStats(d, "STATS: Arestas Relat >0.2");
        //printStats(d, "STATS: Arestas Relat >0.15");
        printStats(d5, "Arestas Relacionadas");

        ArrayList<DescriptiveStatistics> ds = new ArrayList<>();
        ArrayList<String> nams = new ArrayList<>();
        ds.add(d);
        nams.add(">0.95");
        ds.add(d2);
        nams.add(">0.9");
        ds.add(d3);
        nams.add(">0.8");
        ds.add(d4);
        nams.add(">0.6");
        ds.add(d5);
        nams.add(">0");
        ds.add(d6);
        nams.add(">0.4");
        ds.add(d7);
        nams.add(">0.3");
        GeraGraficos g = new GeraGraficos(1200, 800);
        //g.GeraCDF2_From_1_DescpStats(pastaGraficos, "xx", d7, nams);
        g.GeraCDF_FromDescpStats(pastaGraficos, "CDF de número de arestas relacionadas", ds, nams);

        //encontrando arestas agregadoras
        //aresta agregadora = peso de arestas para ela é bem maior que o peso dela para arestas
        //pelo menos 20 relações
        //fator de agregação:   (media fator inbound para (in>0.5)) / (media fator out para arestas)
        System.out.println("PROC: Iniciando cálculo de arestas agregadoras... " + horaAtual());
        int index;
        DescriptiveStatistics agr = new DescriptiveStatistics();
        DescriptiveStatistics agrC = new DescriptiveStatistics();

        double[] fatorAgregador = new double[from.size()];
        int[] contAgregador = new int[from.size()];

        ArrayList<Double>[] inFactor = new ArrayList[from.size()];
        ArrayList<Integer>[] inIndex = new ArrayList[from.size()];
        for (int x = 0; x < from.size(); x++) {
            inFactor[x] = new ArrayList<>();
            inIndex[x] = new ArrayList<>();
        }

        for (int a = 0; a < from.size(); a++) {//de aresta em aresta

            //encontra arestas IN (entrada)
            for (int i = 0; i < from.size(); i++) {
                index = arestasRelac[i].indexOf(a);
                if (index != -1) { //caso seja uma aresta IN de I para A
                    if (contArestasRelac[i].get(index) > 0.5) { //se possui o fator mínimo de relevância
                        inIndex[a].add(i);
                        inFactor[a].add(contArestasRelac[i].get(index));
                        contAgregador[a]++;

                    }
                }
            } //encontrou todas as arestas chegando em A

            //verifica fatores para essas arestas
            for (int i = 0; i < inFactor[a].size(); i++) {
                index = arestasRelac[a].indexOf(inIndex[a].get(i));
                if (index != -1) {
                    //calcula fator Agregador
                    if (inFactor[a].get(i) > (arestasRelac[a].get(index) * 2)) {
                        fatorAgregador[a] += inFactor[a].get(i) / arestasRelac[a].get(index);

                    }
                }
            }

            if (contAgregador[a] > 0) {
                fatorAgregador[a] = (fatorAgregador[a] / contAgregador[a]);

            }

            agr.addValue(fatorAgregador[a]);
            agrC.addValue(contAgregador[a]);

            if (a % (from.size() / 20) == 0) {
                System.out.println("Progress: " + a + "/" + from.size() + " arestas processadas... " + horaAtual());
            }
        }

        System.out.println("OK: Calculou fatores agregadores das arestas. Iniciando escrita de arquivo... " + horaAtual());
        //forma string com pontos críticos de agregação
        double limit = agr.getPercentile(99.7);
        double limit2 = agrC.getPercentile(99.8);
        double limit3 = 200.0;

        String tx = "", td = "aresta, fator, ocorr, lat1, lon1, lat2, lon2\n";
        String tx2 = "", td2 = "aresta, fator, ocorr, lat1, lon1, lat2, lon2\n";
        String tx3 = "", td3 = "aresta, fator, ocorr, lat1, lon1, lat2, lon2\n";

        for (int a = 0; a < from.size(); a++) {
            if (fatorAgregador[a] > limit) {
                tx = tx + mapa.getNode_lat()[from.get(a)] + "," + mapa.getNode_lon()[from.get(a)] + "\n" + mapa.getNode_lat()[to.get(a)] + "," + mapa.getNode_lon()[to.get(a)] + "\n";
                td = td + a + "," + fatorAgregador[a] + "," + contAgregador[a] + "," + mapa.getNode_lat()[from.get(a)] + "," + mapa.getNode_lon()[from.get(a)] + "," + mapa.getNode_lat()[to.get(a)] + "," + mapa.getNode_lon()[to.get(a)] + "\n";
            }
            if (contAgregador[a] > limit2) {
                tx2 = tx2 + mapa.getNode_lat()[from.get(a)] + "," + mapa.getNode_lon()[from.get(a)] + "\n" + mapa.getNode_lat()[to.get(a)] + "," + mapa.getNode_lon()[to.get(a)] + "\n";
                td2 = td2 + a + "," + fatorAgregador[a] + "," + contAgregador[a] + "," + mapa.getNode_lat()[from.get(a)] + "," + mapa.getNode_lon()[from.get(a)] + "," + mapa.getNode_lat()[to.get(a)] + "," + mapa.getNode_lon()[to.get(a)] + "\n";
            }

            if (contAgregadorSuper[a] > limit3) {
                tx3 = tx3 + mapa.getNode_lat()[from.get(a)] + "," + mapa.getNode_lon()[from.get(a)] + "\n" + mapa.getNode_lat()[to.get(a)] + "," + mapa.getNode_lon()[to.get(a)] + "\n";
                td3 = td3 + a + "," + fatorAgregador[a] + "," + contAgregador[a] + "," + mapa.getNode_lat()[from.get(a)] + "," + mapa.getNode_lon()[from.get(a)] + "," + mapa.getNode_lat()[to.get(a)] + "," + mapa.getNode_lon()[to.get(a)] + "\n";
            }

        }
        //salvar arquivo visualização
        salvarTxt("arestas agregadoras gargalos.txt", tx);
        salvarTxt("arestas agregadoras gargalos DETAILS.csv", td);

        salvarTxt("arestas agregadoras gargalos CONT.txt", tx2);
        salvarTxt("arestas agregadoras gargalos DETAILS CONT.csv", td2);

        salvarTxt("arestas altamente relacionadas.txt", tx3);
        salvarTxt("arestas altamente relacionadas DETAILS.csv", td3);

        System.out.println("PROC: Gerando arquivos para importação para GEPHI... " + horaAtual());
        String[] texts = new String[to.size()];

        String tex = "Source, Target, Weight\n";
        for (int a = 0; a < to.size(); a++) {
            texts[a] = "";
            for (int r = 0; r < arestasRelac[a].size(); r++) {
                if (contArestasRelac[a].get(r) > 0.25) {
                    texts[a] = texts[a] + "a" + a + ", " + "a" + arestasRelac[a].get(r) + ", " + contArestasRelac[a].get(r) + "\n";
                }
                //tex = tex + "a"+a + ", " + "a"+arestasRelac[a].get(r) + ", " + contArestasRelac[a].get(r) + "\n";
            }
        }

        for (int a = 0; a < to.size(); a++) {
            tex = tex + texts[a];
        }

        salvarTxt("edges import to GEPHI.csv", tex);

        String nos = "Node\n";
        for (int a = 0; a < to.size(); a++) {
            if (contAresta[a] > 0) {
                nos = "a" + a + "\n";
            }
        }
        salvarTxt("nos import to GEPHI.csv", nos);

        System.out.println("END! Em loop infinito agora...");

        taxiBatches = reserve;

        while (true) {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    } //FIM RELACIONAMENTO ENTRE ARESTAS

    public static void printStats(ArrayList<Double> d, String name) {

        DescriptiveStatistics ds = new DescriptiveStatistics();
        for (int x = 0; x < d.size(); x++) {
            ds.addValue(d.get(x));
        }
        printStats(ds, name);
    }

    public static void printStats(DescriptiveStatistics d, String name) {

        String t = "STATS: " + name + ": Min. " + df2.format(d.getMin()) + "; Mean. " + df2.format(d.getMean());

        for (int p = 5; p < 100; p = p + 15) {
            t = t + "p" + p + " " + df2.format(d.getPercentile(p)) + "; ";
        }

        System.out.println(t + "; Max. " + df2.format(d.getMax()) + "; ");
    }

    public static void demonstrarCaminhosTaxis() {
        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat

        }

        loadTaxiData(0, 10000); //caso não leia, inicia processamento para gerar novo arquivo dat
        mapTaxiTrips(true, false, 6.0, 0.5, 30.0, 5);//mapTaxiTrips(true,true,7.0,2.0,30.0);
        reconstruirCaminhosMap();

        double bomPerc = 0;
        int bonsNos = 0;
        double medPerc = 0;
        ArrayList<Double> percs = new ArrayList<>();

        for (int v = 0; v < cont_viagens_Taxi; v++) {

            percs.add(viagens_Taxi[v].getPercConexao());

            if (viagens_Taxi[v].getPercConexao() > bomPerc) {
                bomPerc = viagens_Taxi[v].getPercConexao();
            }
            if (viagens_Taxi[v].getNodesCount() > bonsNos) {
                bonsNos = viagens_Taxi[v].getNodesCount();
            }
        }

        printStats(percs, "percs viagens");

        bomPerc = bomPerc * 0.96;
        bonsNos = bonsNos / 4;
        criaDiretorio("demonstracao_mapa");
        for (int v = 0; v < cont_viagens_Taxi; v++) {
            if (viagens_Taxi[v].getPercConexao() > bomPerc && viagens_Taxi[v].getNodesCount() > bonsNos) {
                salvarTxt("demonstracao_mapa\\Viagem " + v + " trace " + viagens_Taxi[v].getPercConexao() + "pc " + viagens_Taxi[v].getNodesCount() + " nods.txt",
                        viagens_Taxi[v].demonstrarCaminhoEmMapa(mapa));
                salvarTxt("demonstracao_mapa\\Viagem " + v + " nodes " + viagens_Taxi[v].getPercConexao() + "pc " + viagens_Taxi[v].getNodesCount() + " nods.txt",
                        viagens_Taxi[v].demonstrarNosEmMapa(mapa));
            }
        }

    }

    public static void processarVirtSensorsEODMatrixPorBatches() {

        boolean novoMap = false;

        if (usandoOSMData) {
            System.out.println("PROC: Processamento de VirtualSensores e OD-Matrix por batches: " + linesToRead + " (x " + taxiBatches + " = " + taxiBatches * linesToRead + ") " + horaAtual());

            // Converter e indexar localização de vias OSM
            if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
                loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
                novoMap = true;
            }

            mapa.criarROIs();
            novoOSMDat = true;

            if (registrosPorMinuto == 0) {
                System.out.println("ERROR: registrosPorMinuto == 0. Divisão por zero futura!");
            }

            if (!lerArquivoVirtualSensors(0)) {
                virtualSensors = new VirtualSensors(discretTemporal, mapa, registrosPorMinuto, VirtualSensorsFile); //(String[] nodes, int contNod, double coberSensr, int discretTemporal) 
                virtualSensors.criarArestasDirecionadas(mapa, taxiBatches);
                mapa.cadastrarVizinhos();

            } else {
                virtualSensors.zerarInfs();

                if (novoMap) {
                    mapa.cadastrarVizinhos();
                }

            }
            saveFiles(false, false, false);

            createODMatrix();

            if (!utilizarKmeans) {
                ODmatrix.loadDataset5RedefinirClusters("dataset5.arff");
                ODmatrix.resetContagensBlocos(taxiBatches, ODmatrix.getNumeroClusters());
            }

            if (!usandoTrafficCounts) {
                for (int bat = 0; bat < taxiBatches; bat++) {
                    //carregar dados de taxi
                    loadTaxiData(bat * linesToRead, linesToRead); //caso não leia, inicia processamento para gerar novo arquivo dat
                    mapTaxiTrips(true, false, 6.0, 0.5, 30.0, 5);//mapTaxiTrips(true,true,7.0,2.0,30.0);
                    reconstruirCaminhosMap();
                    repassarViagensParaSensores(bat);

                    //parte Od-Matrix
                    identificarQuadrantesOD(); //Calculos nos objetos de viagem_Taxi
                    acrescentarDadosDeTaxiNaODMatrix(bat);

                    System.out.println("OK: Fim batch " + (bat + 1) + "/" + taxiBatches + "! (" + tok + "/" + tv + " viagens repassadas)" + horaAtual() + "\n");
                }
                virtualSensors.getArestasMaismovimentadasIndex(7500, 24); //para evitar processamento repetitivo

                //ODmatrix.normalizarPathLinkMatrix(discretTemporal);
                if (utilizarKmeans) {
                    ODmatrix.criarClusters();
                } else {
                    ODmatrix.calcMatrixClust();
                }
                ODmatrix.normalizarPathLinkMatrix(discretTemporal);
                ODmatrix.encontrarNosBordaClusters(mapa);

            } else { //usando arquivo especifico

                virtualSensors.lerArquivoVolumesNYC(mapa, VirtualSensorsFile);

            }

        } else { //nao usando OSM data = estudo de caso

            geradordeRede2 gerador = new geradordeRede2();
            System.out.println("PROC: Processamento de VirtualSensores, utilizando HARDCODE " + horaAtual());

            if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
                loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
                novoMap = true;
            }

            if (!lerArquivoVirtualSensors(0)) {
                virtualSensors = new VirtualSensors(discretTemporal, mapa, 0, VirtualSensorsFile); //(String[] nodes, int contNod, double coberSensr, int discretTemporal) 
                virtualSensors.criarArestasDirecionadas(mapa, taxiBatches);
                mapa.cadastrarVizinhos();
                novoOSMDat = true;
                saveFiles(false, false, false);
            } else {
                virtualSensors.zerarInfs();

                if (novoMap) {
                    mapa.cadastrarVizinhos();
                }

            }

            //Colocando contadores nos sensores/links
            int codFrom;
            int codTo;
            for (int c = 0; c < gerador.getContArestas(); c++) {

                codFrom = mapa.getNodeIndex(gerador.getFromNoC().get(c));
                codTo = mapa.getNodeIndex(gerador.getToNoC().get(c));

                for (int g = 0; g < gerador.getContLink().get(c); g++) {

                    virtualSensors.addContREDUX(codFrom, codTo, 0, 0);

                }
            }

            //aproveita para criar arquivo OD-Matrix
            System.out.println("PROC: Processamento de O-D Matrix. Harcoded, estudo de caso  " + horaAtual());

            if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
                loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
            }

            mapa.criarROIs();
            novoOSMDat = true;

            createODMatrix();

            ODmatrix.setODMatrixHardCode(gerador.getMatrizPriori(), gerador.getNumPontosOD());
            ODmatrix.definirCaminhosRedeFechada(mapa, virtualSensors, gerador.getNumPontosOD());

        }

        /*if (descobrirLimitesTaxisTrips) { descobrirLimites();    summaryViagens(); //calculos em viagens_Taxi  } System.out.println();*/
        saveFiles(false, true, true);

    }

    public static void testarDiferentesMapMatching() {
        String best = "";
        String actual;
        String all = "";
        int n = 2000;
        double bestFit = 0.0;

        System.out.println("PROC: Testando diferentes configs map matching. " + n + " viagens");
        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }

        for (int porAresta = 0; porAresta <=1; porAresta++) {
        for (int prof = 0; prof <= 0; prof=prof+3) {        
            for (double raioM = 8; raioM <= 25.0; raioM += 2) {
                
                profundidadeBuscaCompletarCaminhos = 18 + prof;
                
                if(porAresta==0 || prof ==0){ //porNos nao precisa testar profundidade de arestas.
                //carregar dados de taxi
                loadTaxiData(0, n); //caso não leia, inicia processamento para gerar novo arquivo dat
                mapTaxiTrips(true, porAresta == 1, raioM, 0.0, 30.0, prof); //rigidoEmCleaReg, porArestas, metrosRaioNode, double rand, double distValidacao, profundidadeBuscaArestas
                reconstruirCaminhosMap();
                //repassarViagensParaSensores(bat);
                actual = "porAresta=" + (porAresta == 1) + "; Prof = "+prof+"; raioM= " + raioM + "; Conexao = " + calcFitnessMapMatching() + "%; NosMed =" + nosC + "; viagens = "+cont_viagens_Taxi+ "; "+horaAtual();
                all = all + actual + "\n";
                if (calcFitnessMapMatching() > bestFit) {
                    bestFit = calcFitnessMapMatching();
                    best = actual;
                }
                System.out.println(horaAtual()+": "+ actual);
                }
            }
        }
        }

        System.out.println("OK: Testes melhor matching:  " + best + "\n");
        System.out.println(all);
    }
    static double nosC = 0.0;

    public static double calcFitnessMapMatching() {
        double ac = 0.0;

        for (int c = 0; c < cont_viagens_Taxi; c++) {
            //ac = ac + viagens_Taxi[c].getFitnesseConexaoNumNos();
            ac = ac +  viagens_Taxi[c].getPercConexao();
            nosC = nosC + viagens_Taxi[c].getPosNodesEmMapa().size();
        }
        nosC = nosC / cont_viagens_Taxi;
        return ac / cont_viagens_Taxi;
    }

    public static void repassarViagensParaSensores(int bat) {

        if (bat == 0) {
            tv = 0;
            tok = 0;
        }

        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            tok += virtualSensors.distribuirPelosNodes(viagens_Taxi[a].getPosNodesEmMapa(), viagens_Taxi[a].getHora_inicio(), bat);
        }

        if (cont_viagens_Taxi > 0) {
            virtualSensors.printContFound();
        }

        tv += cont_viagens_Taxi - 1;

    }

    static int tv, tok;

    public static void acrescentarDadosDeTaxiNaODMatrix(int bat) {

        if (bat == 0) {
            tv = 0;
            tok = 0;
        }

        System.out.println("PROC: Acrescentando dados para obj O-D Matrix... " + horaAtual());

        int c = 0;

        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            ODmatrix.addViagemNaMatrix(viagens_Taxi[a].getQuadrantePartidaLat(), viagens_Taxi[a].getQuadrantePartidaLon(),
                    viagens_Taxi[a].getQuadranteChegadaLat(), viagens_Taxi[a].getQuadranteChegadaLon(), viagens_Taxi[a].getHora_inicio(), bat);

            //cuidado para não deixar virtualSensors == null
            c += ODmatrix.acrescentarMatrizPathLink(mapa, viagens_Taxi[a].getQuadrantePartidaLat(), viagens_Taxi[a].getQuadrantePartidaLon(),
                    viagens_Taxi[a].getQuadranteChegadaLat(), viagens_Taxi[a].getQuadranteChegadaLon(), viagens_Taxi[a].getPosNodesEmMapa(), viagens_Taxi[a].getPedacDiscretTemporal(discretTemporal));

        }

        tv += cont_viagens_Taxi - 1;
        tok += c;

        System.out.println("OK: Acrescentou dados de (" + c + "/" + (cont_viagens_Taxi - 1) + ") viagens para obj O-D Matrix! (Total: " + tok + "/" + tv + " OK!)   " + horaAtual());

    }

    public static void createODMatrix() {

        System.out.println(mapa.getROInumberLon() + " " + mapa.getMenorLat());

        ODmatrix = new ODMatrix(mapa.getROIsizeM(), mapa.getROInumberLat(), mapa.getROInumberLon(),
                discretTemporal, numeroClusters, minimoContParaMostrarLinhaColunaBlocos,
                mapa.getMenorLat(), mapa.getMenorLon(), taxiBatches); //(int RoiSizeM, int roiNmbLat, int roiNumbLon, int discretizacaoTemporal)

        System.out.println("ODmatrix.setMenorLatLon(" + mapa.getMenorLat() + "," + mapa.getMenorLon() + ")");
        //ODmatrix.setMenorLatLon();

    }

    public static void mapTaxiTrips(boolean rigidoEmCleaReg, boolean porArestas, double metrosRaioNode, double rand, double distValidacao, int profundidadeBuscaArestas) {

        System.out.println("PROC: Mapeando taxi trips... " + horaAtual());

        clearRegistrosRuins(false, rigidoEmCleaReg);

        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            //mapearTrip(Mapping mapa, boolean porArestas, double metrosRaioNode, double rand, double distValidacao)
            viagens_Taxi[a].mapearTrip(mapa, porArestas, metrosRaioNode, rand, distValidacao, profundidadeBuscaArestas);
        }

        clearRegistrosRuins(true, rigidoEmCleaReg);

        DescriptiveStatistics stats = new DescriptiveStatistics();

        //summary de nodes
        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            stats.addValue(viagens_Taxi[a].getNodesCount());
        }
        System.out.println("STATS: Taxi_Trip NODES(un). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

        /*stats = new DescriptiveStatistics();
        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            stats.addValue(viagens_Taxi[a].getWaysCount());
        }
        System.out.println("STATS: Taxi_Trip WAYS(un). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");*/
        System.out.println("END: Mapeou taxi trips... " + horaAtual());

    }

    public static void loadOSMData() {

        if (usandoOSMData) {
            System.out.println("PROC: Recuperando dados geográficos do xml OSM... " + horaAtual());
            /*Scanner scanner;
            try {
                scanner = new Scanner(new File(OSMFileLocation));
                scanner.useDelimiter("\\Z");*/
            mapa = new Mapping(OSMFileLocation, calcWaysPorNode, ROIsizeM, metrosMaxParaSerNodeGemeo, "");
            novoOSMDat = true;

            /*} catch (FileNotFoundException ex) {
                Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            System.out.println("END: Recuperou dados e criou objetos geográficos! " + horaAtual());

        } else { // Nao utilizando mapa
            System.out.println("PROC: Nao utiliza xml OSM! Gerando mapa por hardcode. " + horaAtual());
            mapa = new Mapping(ROIsizeM, "");
            novoOSMDat = true;
        }

        //não existirá mais o problema de 
        saveFiles(false, false, false);
        novoOSMDat = false;

    }

    public static void loadTaxiData(int start, int limite) {

        //System.out.println("PROC: Recuperando dados de " + linesToRead + " linhas do csv de taxis trips... " + horaAtual());
        //String[] sep;
        //sep = new String[linesToRead];
        Scanner scanner;
        String[] r;
        String aux;
        viagens_Taxi = new taxi_Trip_Instance[limite - start + 1];
        int c = 0;

        try {
            scanner = new Scanner(new File(TaxiFileLocation));
            scanner.useDelimiter("\n");

            while (c <= (limite)) {
                //sep[c] = scanner.next();

                aux = scanner.next();
                if (c > start) {
                    r = aux.split(",");
                    viagens_Taxi[c - 1 - start] = new taxi_Trip_Instance(r, metrosRaioNode, registrosPorMinuto);
                }
                c++;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

        cont_viagens_Taxi = c - start;

        /*(for (int a = 1; a < sep.length; a++) {
            r = sep[a].split(",");
            viagens_Taxi[a-1] = new taxi_Trip_Instance(r);
        }*/
        // System.out.println("END: Recuperou dados e criou objetos de taxi_trip! " + horaAtual());
    }

    public static String leArquivo(String arquivo) {
        Scanner scanner;
        String r = "";
        try {
            scanner = new Scanner(new File(arquivo));
            //  scanner.useDelimiter("\\Z");
            // return scanner.next();
            scanner.useDelimiter("\n");
            int c = 0;
            while (c < linesToRead) {
                r = r + scanner.next();
                c++;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "fail";
    }

    public static String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));
    }

    private static void descobrirLimites() {

        double menorLat, maiorLat, menorLon, maiorLon;

        menorLat = viagens_Taxi[0].getLat(0);
        maiorLat = menorLat;
        menorLon = viagens_Taxi[0].getLon(0);
        maiorLon = menorLon;
        double aux;

        for (int a = 1; a < (cont_viagens_Taxi - 1); a++) {
            aux = viagens_Taxi[a].getLat(0);

            if (aux < menorLat && aux != -1) {
                menorLat = aux;
            } else if (aux > maiorLat && aux != -1) {
                maiorLat = aux;
            }

            aux = viagens_Taxi[a].getLon(0);

            if (aux < menorLon && aux != -1) {
                menorLon = aux;
            } else if (aux > maiorLon && aux != -1) {
                maiorLon = aux;
            }

        }

        System.out.println("RSLT: Inicios de viagem em Lat [" + menorLat + "; " + maiorLat + "], Lon [" + menorLon + "; " + maiorLon + "] ");

    }

    private static void saveFiles(boolean salvaTaxiFile, boolean salvarODMatrix, boolean salvarVirtualSensors) {

        System.out.println("PROC: Iniciando gravação de objetos em disco... " + horaAtual());

        if (novoOSMDat) {

            mapa.salvarDat(OSMFileLocation/*.split(".")[0]*/);
        }

        String p = TaxiFileLocation/*.split(".")[0]*/ + " " + linesToRead + ".dat";

        if (salvaTaxiFile) {

            try {
                FileOutputStream arquivoGrav = new FileOutputStream(p);
                ObjectOutputStream objGravar = new ObjectOutputStream(arquivoGrav);
                objGravar.writeObject(new taxi_trip_array(viagens_Taxi));
                objGravar.flush();
                objGravar.close();
                arquivoGrav.flush();
                arquivoGrav.close();
                System.out.println("OK: Dados de taxi '" + p + "' salvos em .dat!");
            } catch (IOException e) {
                // e.printStackTrace();
                System.out.println("ERROR: Falha ao salvar .dat de " + p);
            }

        }

        if (ODmatrix != null && salvarODMatrix) {
            ODmatrix.salvarDat(ODMatrixFileLocation + " " + tempoInicio + "a" + tempoFim + "de" + discretTemporal);
            ODmatrix.salvarCsv(ODMatrixFileLocation + " " + tempoInicio + "a" + tempoFim + "de" + discretTemporal, todosDetalhesBlocosCsvODmatrix);
        }

        if (salvarVirtualSensors) {
            virtualSensors.salvarDat(VirtualSensorsFile);
            virtualSensors.salvarCsv(VirtualSensorsFile + " " + tempoInicio + "a" + tempoFim + "de" + discretTemporal, tempoInicio, tempoFim, mapa);
            //virtualSensors.gerarScriptLocaisMovimentados(mapa);
        }

        System.out.println("END: Processo de gravação concluído! -  " + horaAtual() + "\n");

    }

    private static boolean lerArquivoOSMDat() {

        if (mapa != null) {
            return true;
        }

        try {

            ObjectInputStream objectIn
                    = new ObjectInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(OSMFileLocation + ".dat")));
            mapa = (Mapping) objectIn.readObject();

            objectIn.close();

            /*try (FileInputStream arquivoLeitura = new FileInputStream(OSMFileLocation + ".dat")) {
                ObjectInputStream objLeitura = new ObjectInputStream(arquivoLeitura);

                mapa = (Mapping) objLeitura.readObject();
                mapa.setROIsizeM(ROIsizeM);

                objLeitura.close();

            }*/
            System.out.println("OK: Recuperou objeto mapa de '" + OSMFileLocation + ".dat'! (" + mapa.getWayCount() + " ways, " + mapa.getContNodes() + " nodes) " + horaAtual() + "\n");
            return true;
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("ALERT: Não recuperou registros de arquivo '" + OSMFileLocation + ".dat'");
            return false;
        }
    }

    private static boolean lerArquivoODMatrixDat() {

        if (ODmatrix != null) {
            return true;
        }

        try {

            ObjectInputStream objectIn
                    = new ObjectInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(ODMatrixFileLocation + " " + tempoInicio + "a" + tempoFim + "de" + discretTemporal + ".dat")));
            ODmatrix = (ODMatrix) objectIn.readObject();
            objectIn.close();

            /*try (FileInputStream arquivoLeitura = new FileInputStream(ODMatrixFileLocation + " " + tempoInicio + "a" + tempoFim + "de" + discretTemporal + ".dat")) {
                ObjectInputStream objLeitura = new ObjectInputStream(arquivoLeitura);
                ODmatrix = (ODMatrix) objLeitura.readObject();
                objLeitura.close();
            }*/
            System.out.println("OK: Recuperou objeto ODMatrix de '" + ODMatrixFileLocation + " " + tempoInicio + "a" + tempoFim + "de" + discretTemporal + ".dat'! " + horaAtual() + "\n");
            return true;
        } catch (Exception e) {
            // e.printStackTrace(); //ashuas
            System.out.println("ALERT: Não recuperou registros de arquivo '" + ODMatrixFileLocation + " " + tempoInicio + "a" + tempoFim + "de" + discretTemporal + ".dat'");
            return false;
        }
    }

    private static void clearRegistrosRuins(boolean testeNodes, boolean rigido) {
        taxi_Trip_Instance[] aux;

        ArrayList<String> motivos = new ArrayList<>();
        ArrayList<Integer> motivosCont = new ArrayList<>();

        boolean[] sav = new boolean[linesToRead];
        int cont = 0;
        int minInicio = (60 * 24 / discretTemporal) * tempoInicio;
        int minFim = (60 * 24 / discretTemporal) * tempoFim;

        int minInicio2 = (60 * 24 / discretTemporal) * tempoInicio2;
        int minFim2 = (60 * 24 / discretTemporal) * tempoFim2;

        for (int a = 0; a < (cont_viagens_Taxi - 1); a++) {
            sav[a] = viagens_Taxi[a].deve_ser_mantida(minMinCorrida, mantemComMissing_data, weekDays, weekEnd, minimoNodes, testeNodes, minInicio, minFim, minInicio2, minFim2, rigido);
            if (sav[a]) {
                cont++;
            }
        }

        aux = new taxi_Trip_Instance[cont];
        cont = 0;

        for (int a = 0; a < (cont_viagens_Taxi - 1); a++) {
            if (sav[a]) {
                aux[cont] = viagens_Taxi[a];
                cont++;
            } else {
                int ind = motivos.indexOf(viagens_Taxi[a].getMotivoExclusao());
                if (ind != -1) {
                    motivosCont.set(ind, motivosCont.get(ind) + 1);
                } else {
                    motivos.add(viagens_Taxi[a].getMotivoExclusao());
                    motivosCont.add(1);
                }

            }
        }

        String motivs = "";
        for (int m = 0; m < motivos.size(); m++) {
            motivs = motivs + motivos.get(m) + "=" + motivosCont.get(m) + "; ";
        }

        if ((cont_viagens_Taxi - 1) != cont) {
            System.out.println("ADQ: Reduziu viagens_Taxi de " + (cont_viagens_Taxi - 1) + " para " + cont + ". (" + horaAtual() + ") " + motivs);

            viagens_Taxi = aux;
            cont_viagens_Taxi = cont;
        }

    }

    private static void summaryViagens() {

        DescriptiveStatistics stats = new DescriptiveStatistics();
        double aux;
        int fail = 0;

// Add the data from the array
        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            aux = viagens_Taxi[a].distanciaDaCorridaM();
            if (aux != -1) {
                stats.addValue(aux);
            } else {
                fail++;
            }
        }

        System.out.println("STATS: Taxi_Trip Distance(Meters). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + "; Fail read: " + fail);

        stats = new DescriptiveStatistics();
        fail = 0;
        // Add the data from the array
        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            stats.addValue(viagens_Taxi[a].duracaoCorridaMin());
        }

        System.out.println("STATS: Taxi_Trip Time(Minutes). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + "; Fail read: " + fail);
    }

    private static void identificarQuadrantesOD() {

        if (mapa == null) {
            System.out.println("ERROR: Objeto mapa == null!");
        } else if (viagens_Taxi == null) {
            System.out.println("ERROR: Array viagens_Taxi == null!");
        } else if (cont_viagens_Taxi == 0) {
            System.out.println("ERROR: cont_viagens_Taxi.lenght == 0!");
        } else if (viagens_Taxi[0] == null) {
            System.out.println("ERROR: Array viagens_Taxi[0] == null!");
        } else if (ROIsizeM <= 0) {
            System.out.println("ERROR: ROIsizeM <= 0");
        }

        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            viagens_Taxi[a].calculaQuadrantesODMatrix(mapa.getMenorLat(), mapa.getMenorLon(), ROIsizeM, mapa.getROInumberLat(), mapa.getROInumberLon());
        }

    }

    private static boolean lerArquivoVirtualSensors(int cont) {

        if (virtualSensors != null) {
            return true;
        }

        try {

            ObjectInputStream objectIn
                    = new ObjectInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(VirtualSensorsFile + ".dat")));
            virtualSensors = (VirtualSensors) objectIn.readObject();

            objectIn.close();

            /*
            try (FileInputStream arquivoLeitura = new FileInputStream(VirtualSensorsFile + ".dat")) {
                ObjectInputStream objLeitura = new ObjectInputStream(arquivoLeitura);
                virtualSensors = (VirtualSensors) objLeitura.readObject();
                //objLeitura.close();
                arquivoLeitura.close();

            } */
            System.out.println("OK: Recuperou objeto VirtualSensors de '" + VirtualSensorsFile + ".dat'! " + horaAtual() + "\n");
            return true;
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("ALERT: Não recuperou objeto de arquivo '" + VirtualSensorsFile + ".dat'");
            if (cont > 3) {
                return false;
            } else {
                try {
                    Thread.sleep(10000);
                    return lerArquivoVirtualSensors(cont + 1);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }

            }

        }
    }

    public static void reconstruirCaminhosMap() {
        //construirCaminhosIncompletos(ArrayList<String>[] way_nodes, boolean[] oneWay, int contWays)
        DescriptiveStatistics stats = new DescriptiveStatistics();
        int perc = cont_viagens_Taxi / 10;

        System.out.println("PROC: Iniciando reconstrução de caminhos... " + horaAtual());
        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {
            stats.addValue(viagens_Taxi[a].construirCaminhosIncompletos(mapa, usarNodesGemeosComoVizinhos, false, false, false, profundidadeBuscaCompletarCaminhos));

            if (a % perc == 0 && a > 0) {
                System.out.println("Ok... " + (100 * a / cont_viagens_Taxi) + "% caminhos reconstruidos! " + horaAtual());
            }

            if (showDetalheViagens) {
                viagens_Taxi[a].demonstrarCaminhoEmMapa(mapa);
            }

            //   System.out.println(viagens_Taxi[a].getNecessidadesInclude() + " necessidades, "+ viagens_Taxi[a].getIncludesEfetivados()+ " includes feitos.");
        }

        printStats(stats, "Reconstrução de caminho Taxi_Trip (Nodes)");
        //System.out.println("STATS: Reconstrução de caminho Taxi_Trip (Nodes). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

        //identificar nodes com indice -1 em tax_Trips
    }

    public static void calcularDistanciasNodesDasViagens() {
        //construirCaminhosIncompletos(ArrayList<String>[] way_nodes, boolean[] oneWay, int contWays)
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int a = 0; a < cont_viagens_Taxi - 1; a++) {

            for (int n = 0; n < viagens_Taxi[a].getNodesCount() - 1; n++) {
                stats.addValue(mapa.distanciaMNodes(viagens_Taxi[a].getPosNodesEmMapa().get(n), viagens_Taxi[a].getPosNodesEmMapa().get(n + 1)));
            }

        }

        System.out.println("STATS: Distancia entre nodes da Taxi_Trip (M). Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

        //identificar nodes com indice -1 em tax_Trips
    }

    public static void calcularStatsDistanciaNodesMap(Mapping map) {

        System.out.println("PROC: Calculando distâncias mínimas entre nodes... " + horaAtual());

        DescriptiveStatistics stats = new DescriptiveStatistics();

        //String[] node_id = mapa.getNode_id();
        Double[] node_lat = mapa.getNode_lat();
        Double[] node_lon = mapa.getNode_lon();
        int cont = mapa.getContNodes();

        //de node em node, encontra outro node com a menor distãncia
        double menorD;
        double aux;
        for (int n = 0; n < cont; n++) {
            menorD = 99;

            for (int n2 = 0; n2 < cont; n2++) {
                //  System.out.println("n="+n+"; n2="+n2);

                aux = map.distanciaPontosMETROS(node_lat[n], node_lon[n], node_lat[n2], node_lon[n2]);
                if (n != n2 && aux < menorD) {
                    menorD = aux;
                }

            }

            stats.addValue(menorD / 0.0000089);

        }

        System.out.println("STATS: Node mais próximo (M). Min: " + df2.format(stats.getMin()) + "; perc5: " + df2.format(stats.getPercentile(5)) + "; perc10: " + df2.format(stats.getPercentile(10)) + "; perc15: " + df2.format(stats.getPercentile(15)) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

    }

    private static void gerarDatasetsDiferenciados() {

        System.out.println("PROC: Criação de datasets diferenciados: " + linesToRead + " (x " + taxiBatches + " = " + taxiBatches * linesToRead + ") " + horaAtual());

        //dataset 1: instanciaDatasetODTempo()  - Retirada simples dos objetos taxi_Trip_Instance  (anomalias)
        //dataset 2: Agrupamento por dia - Criar vetores, acumular com os batches  (anomalias)
        //dataset 3: Dados de taxistas
        //dataset 4: Regressão linear - Criar dataset que indique por que regiões/blocos a viagem passou
        //dataset 5: Chamadas O e D, localização 2D  (clustering)
        String cabe1 = "latIn, lonIn, latF, lonF, tempoIn, tempoF, weekday, taxi_id";
        String cabe2 = "dia, di5, di10, di20, dist35, dist50, di65, dit80, di90, di95, t5, t10, t20, t35, t50, t65, t80, t90, t95, weekday, cont";
        String cabe3 = "taxi, distancia, tempo, viagens, velMed kmh, distMed, tempMed";
        String cabe4;
        String cabe5 = "lat, lon, tipo";

        String cb1t;
        String cb2t;
        String cb4t;
        String cb5t;

        String dataset1 = cabe1 + "\n";
        String dataset3 = cabe3 + "\n";
        String dataset4 = "";
        String dataset5 = cabe5 + "\n";

        // Converter e indexar localização de vias OSM
        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }

        mapa.criarROIs();
        novoOSMDat = true;
        tempoVet = new ArrayList<>();
        distVet = new ArrayList<>();
        diaVet = new ArrayList<>();
        weekdayVet = new ArrayList<>();

        ArrayList<String> taxista = new ArrayList<>();
        ArrayList<Double> distanciaPer = new ArrayList<>();
        ArrayList<Double> tempoDir = new ArrayList<>();
        ArrayList<Integer> viagens = new ArrayList<>();

        createODMatrix();

        for (int bat = 0; bat < taxiBatches; bat++) {

            cb1t = "";
            cb4t = "";
            cb5t = "";

            loadTaxiData(bat * linesToRead, linesToRead); //caso não leia, inicia processamento para gerar novo arquivo dat

            if (bat == 0) {
                cabe4 = viagens_Taxi[1].getCabecPassaBlocos(mapa.getROInumberLat(), mapa.getROInumberLon());
                dataset4 = cabe4 + "\n";
            }

            if (selecionarRegistrosParaDatasetsDif) {
                clearRegistrosRuins(false, false);
            }

            //mapTaxiTrips(false);
            for (int x = 0; x < cont_viagens_Taxi - 1; x++) {
                cb1t = cb1t + viagens_Taxi[x].instanciaDatasetODTempo();
                cb4t = cb4t + viagens_Taxi[x].getInstanciaPassaBlocos(mapa.getMenorLat(), mapa.getMenorLon(), ((double) mapa.getROIsizeM()) * 0.0000089, mapa.getROInumberLat(), mapa.getROInumberLon());
                cb5t = cb5t + viagens_Taxi[x].instanciaLocalizacao();
                //dataset3
                int ind = taxista.indexOf(viagens_Taxi[x].getTaxi_id());
                if (ind != -1) {
                    distanciaPer.set(ind, distanciaPer.get(ind) + viagens_Taxi[x].distanciaDaCorridaM());
                    tempoDir.set(ind, tempoDir.get(ind) + viagens_Taxi[x].duracaoCorridaMin());
                    viagens.set(ind, viagens.get(ind) + 1);
                } else {
                    taxista.add(viagens_Taxi[x].getTaxi_id());
                    distanciaPer.add(viagens_Taxi[x].distanciaDaCorridaM());
                    tempoDir.add(viagens_Taxi[x].duracaoCorridaMin());
                    viagens.add(1);
                } //fim dataset3

            }

            dataset1 = dataset1 + cb1t;
            dataset4 = dataset4 + cb4t;
            acumularDadosDiaDataset2();
            dataset5 = dataset5 + cb5t;

            // identificarQuadrantesOD(); //Calculos nos objetos de viagem_Taxi // acrescentarDadosDeTaxiNaODMatrix();
            System.out.println("OK: Fim batch " + (bat + 1) + "/" + taxiBatches + "! (dataset1) " + horaAtual() + "\n");

        }

        double[] velMed = new double[taxista.size()];
        double[] disMed = new double[taxista.size()];
        double[] temMed = new double[taxista.size()];
        int er = 0;

        for (int t = 0; t < taxista.size(); t++) {

            if (tempoDir.get(t) > 0) {
                velMed[t - er] = 3.6 * (distanciaPer.get(t) / (tempoDir.get(t) * 60));
            } else {
                er++;
            }

            disMed[t] = (distanciaPer.get(t) / viagens.get(t));
            temMed[t] = (tempoDir.get(t) / viagens.get(t));

            dataset3 = dataset3 + taxista.get(t) + ", " + distanciaPer.get(t) + ", " + tempoDir.get(t) + ", " + viagens.get(t) + ", "
                    + velMed[t] + ", " + disMed[t] + ", " + temMed[t] + "\n";
        }

        double[] aux = new double[velMed.length - er];
        System.arraycopy(velMed, 0, aux, 0, velMed.length - er);
        velMed = aux;

        GeraGraficos gx = new GeraGraficos(1000, 800);
        gx.criarHistogramaA(pastaGraficos, "Histograma Distância total por taxista", distanciaPer, 15);
        gx.criarHistogramaA(pastaGraficos, "Histograma Tempo total por taxista", tempoDir, 15);
        gx.criarHistogramaI(pastaGraficos, "Histograma Viagens por taxista", viagens, 15);
        gx.criarHistograma(pastaGraficos, "Histograma Velocidade média por taxista", velMed, 15);
        gx.criarHistograma(pastaGraficos, "Histograma Distância média de corrida por taxista", disMed, 15);
        gx.criarHistograma(pastaGraficos, "Histograma Tempo médio de corrida por taxista", temMed, 15);

        salvarTxt("dataset1 " + cabe1 + ".csv", dataset1);
        salvarTxt("dataset2 " + cabe2 + ".csv", gerarDataSet2(cabe2 + "\n"));
        salvarTxt("dataset3 " + cabe3 + ".csv", dataset3);
        salvarTxt("dataset4 BLOCOS.csv", dataset4);
        salvarTxt("dataset5 " + cabe5 + ".csv", dataset5);

    }

    private static void calcularResumoDados() {

        criaDiretorio(pastaPlanilhasResumos);

        int step = 24;
        weekDays = true;
        weekEnd = true;

        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }

        lerArquivoVirtualSensors(0);

        int[] gf = virtualSensors.getArestasMaismovimentadasIndex(32500, -1);

        int[] nosOcupados = juncaoGraus(grausDosNos(virtualSensors.getFromNodIndex(), virtualSensors.getToNodIndex(), mapa.getContNodes(), "Grafo completo"), grausDosNos2(gf, virtualSensors.getArestasMaismovimentadasIndexTONODE(), mapa.getContNodes(), "Grafo das viagens"));
        //weekDays, weekEnd, tempoInicio, tempoFim, discretTemporal
        //  while (weekDays || weekEnd) {
        maisResumoDados();

        for (; step >= 3;) {
            for (tempoInicio = 0; tempoInicio < discretTemporal; tempoInicio += step) {
                tempoFim = tempoInicio + step;
                if (tempoFim > discretTemporal) {
                    tempoFim = discretTemporal;
                }

                weekDays = true;
                weekEnd = false;
                calcularResumoDados2();
                /*    weekDays = false;
                    weekEnd = true;
                    calcularResumoDados2();*/
                weekDays = true;
                weekEnd = true;
                calcularResumoDados2();

            }

            switch (step) {
                case 24:
                    step = 12;
                    break;
                case 12:
                    step = 6;
                    break;
                case 6:
                    step = 3;
                    break;
                default:
                    step = -1;
                    break;
            }

        }

    }

    private static int[] juncaoGraus(double[] grausDosNos, double[] grausDosNos2) {

        int[] graus = new int[15];
        int[] ocupados = new int[80186];
        int ocup = 0;

        for (int x = 0; x < grausDosNos.length; x++) {
            if (grausDosNos2[x] > 0) {
                graus[(int) grausDosNos[x]]++;
                ocupados[ocup] = x;
                ocup++;
            }
        }

        System.out.println("Junção(grau de nos ocupados) ");
        for (int x = 0; x < 10; x++) {
            System.out.print(x + ": " + graus[x]);
        }
        System.out.println();

        return ocupados;
    }

    private static double[] grausDosNos2(int[] from, int[] to, int nd, String name) {
        ArrayList<Integer> x = new ArrayList<>();
        ArrayList<Integer> y = new ArrayList<>();

        for (int i = 0; i < to.length; i++) {
            x.add(from[i]);
            y.add(to[i]);
        }

        return grausDosNos(x, y, nd, name);

    }

    public static double[] grausDosNos(ArrayList<Integer> from, ArrayList<Integer> to, int nd, String name) {

        double[] contGrau = new double[nd];

        for (int x = 0; x < to.size(); x++) {
            contGrau[from.get(x)]++;
            contGrau[to.get(x)]++;
        }

        int[] graus = new int[15];

        for (int x = 0; x < nd; x++) {
            graus[(int) contGrau[x]]++;
        }

        System.out.println(name + " ");
        for (int x = 0; x < 10; x++) {
            System.out.print(x + ": " + graus[x]);
        }
        System.out.println();

        GeraGraficos g = new GeraGraficos(1200, 800);
        g.criarHistograma(pastaGraficos, name, contGrau, 6);

        return contGrau;
    }

    private static void maisResumoDados() {

        /* (Velocidade média)/(distâncias)/(durações)/(chamadas)    ao longo do dia
        Chamadas por dia ao longo do ano*/
        System.out.println("PROC: Calcular dados por dia: " + linesToRead + " (x " + taxiBatches + " = " + taxiBatches * linesToRead + ") " + horaAtual());

        String det = tempoInicio + "at" + tempoFim + "de" + discretTemporal + " wd" + weekDays + " we" + weekEnd;

        //DescriptiveStatistics tempo = new DescriptiveStatistics();
        ArrayList<Double> labelValues = new ArrayList<>();
        ArrayList<Double> labelSequencial = new ArrayList<>();

        double latIn = 0, latFim = 0, lonIn = 0, lonFim = 0;

        double[] tempos1 = new double[1500];
        int contTempos1 = 0;
        double[] tempos2 = new double[500];
        int contTempos2 = 0;

        double[] tempos3 = new double[50];
        int contTempos3 = 0;

        Double[] distanciaD = new Double[48];
        Double[] duracaoD = new Double[48];
        Double[] velocidadeD = new Double[48];
        int[] chamadasD = new int[48];

        ArrayList<String> datas = new ArrayList<>();
        ArrayList<Double> distanciaT = new ArrayList<>();
        ArrayList<Double> duracaoT = new ArrayList<>();
        ArrayList<Double> velocidadeT = new ArrayList<>();
        ArrayList<Integer> chamadasT = new ArrayList<>();

        for (int x = 0; x < 48; x++) {
            distanciaD[x] = 0.0;
            duracaoD[x] = 0.0;
            velocidadeD[x] = 0.0;
            chamadasD[x] = 0;
            labelValues.add(x * 0.5 + 0.25);
        }

        mapa.criarROIs();
        novoOSMDat = true;
        tempoVet = new ArrayList<>();
        distVet = new ArrayList<>();
        diaVet = new ArrayList<>();
        weekdayVet = new ArrayList<>();

        createODMatrix();
        int ind;

        for (int bat = 0; bat < taxiBatches; bat++) {

            loadTaxiData(bat * linesToRead, linesToRead); //caso não leia, inicia processamento para gerar novo arquivo dat

            if (selecionarRegistrosParaDatasetsDif) {
                clearRegistrosRuins(false, false);
            }

            for (int x = 0; x < cont_viagens_Taxi - 1; x++) {
                ind = viagens_Taxi[x].getHoraInt() * 2 + viagens_Taxi[x].getMais30min();
                double distx = viagens_Taxi[x].distanciaDaCorridaM();
                distanciaD[ind] += distx;
                double durX = viagens_Taxi[x].duracaoCorridaMin();
                duracaoD[ind] += durX;
                chamadasD[ind]++;

                if (datas.indexOf(viagens_Taxi[x].getDataInicioString()) != -1) {
                    int ida = datas.indexOf(viagens_Taxi[x].getDataInicioString());

                    distanciaT.set(ida, distanciaT.get(ida) + distx);
                    duracaoT.set(ida, duracaoT.get(ida) + durX);
                    chamadasT.set(ida, chamadasT.get(ida) + 1);

                } else {
                    datas.add(viagens_Taxi[x].getDataInicioString());
                    distanciaT.add(distx);
                    duracaoT.add(durX);
                    chamadasT.add(1);
                }

                if (distx > 1900 && distx < 2100 && durX < 26 && durX >= 1) {

                    contTempos1++;
                    if (contTempos1 > tempos1.length) {
                        double[] aux = new double[contTempos1 * 2];

                        System.arraycopy(tempos1, 0, aux, 0, tempos1.length);
                        tempos1 = aux;
                    }
                    tempos1[contTempos1 - 1] = durX;

                    if (ind == 12) {
                        contTempos2++;
                        if (contTempos2 > tempos2.length) {
                            double[] aux = new double[contTempos2 * 2];

                            System.arraycopy(tempos2, 0, aux, 0, tempos2.length);
                            tempos2 = aux;
                        }
                        tempos2[contTempos2 - 1] = durX;

                        if (contTempos3 == 0 && durX < 16) {
                            contTempos3++;
                            latIn = viagens_Taxi[x].getLat(0);
                            lonIn = viagens_Taxi[x].getLon(0);
                            latFim = viagens_Taxi[x].getLat(viagens_Taxi[x].getCont_pos() - 1);
                            lonFim = viagens_Taxi[x].getLon(viagens_Taxi[x].getCont_pos() - 1);
                            tempos3[contTempos3 - 1] = durX;

                        } else if (proximo(200, latIn, viagens_Taxi[x].getLat(0), lonIn, viagens_Taxi[x].getLon(0), latFim, viagens_Taxi[x].getLat(viagens_Taxi[x].getCont_pos() - 1), lonFim, viagens_Taxi[x].getLon(viagens_Taxi[x].getCont_pos() - 1)) && durX < 16) {

                            contTempos3++;
                            if (contTempos3 > tempos3.length) {
                                double[] aux = new double[contTempos3 * 2];

                                System.arraycopy(tempos3, 0, aux, 0, tempos3.length);
                                tempos3 = aux;
                            }
                            tempos3[contTempos3 - 1] = durX;
                        }
                    }
                }

            }

        }

        for (int d = 0; d < datas.size(); d++) {
            velocidadeT.add(((3.6 * distanciaT.get(d)) / duracaoT.get(d)) / 60);
            distanciaT.set(d, distanciaT.get(d) / chamadasT.get(d));
            duracaoT.set(d, duracaoT.get(d) / chamadasT.get(d));
        }

        for (int x = 0; x < 48; x++) {
            distanciaD[x] = distanciaD[x] / chamadasD[x];
            duracaoD[x] = duracaoD[x] / chamadasD[x];
            velocidadeD[x] = 3.6 * distanciaD[x] / (duracaoD[x] * 60);
        }

        //salvarTxt(pastaPlanilhasResumos+"\\RES percents " + det + ".csv", cb + "\n" + dd + "\n" + dt + "\n" + dm +  "\n\n\n" + cb + "\n" + dd + "\n" + tMin + "\n" + tMin2 + "\n" + tMed + "\n" + tMax + "\n"+ tMax2 + "\n");
        double[] aux = new double[contTempos2];
        System.arraycopy(tempos2, 0, aux, 0, contTempos2);
        tempos2 = aux;
        aux = new double[contTempos1];
        System.arraycopy(tempos1, 0, aux, 0, contTempos1);
        tempos1 = aux;
        aux = new double[contTempos3];
        System.arraycopy(tempos3, 0, aux, 0, contTempos3);
        tempos3 = aux;

        GeraGraficos gx = new GeraGraficos(1000, 800);
        if (tempos1.length > 0) {
            gx.criarHistograma(pastaGraficos, "Histograma d19-21", tempos1, 15);
            if (tempos1.length > 0) {
                gx.criarHistograma(pastaGraficos, "Histograma d19-21 12h", tempos2, 15);
                if (tempos3.length > 0) {
                    gx.criarHistograma(pastaGraficos, "Histograma d19-21 12h OD especifico", tempos3, 8);
                } else {
                    System.out.println("ERRO: Nenhum dado de duração de viagem para a distância E HORÁRIO E OD especifico escolhidos.");
                }

            } else {
                System.out.println("ERRO: Nenhum dado de duração de viagem para a distância E HORÁRIO escolhidos.");
            }

        } else {
            System.out.println("ERRO: Nenhum dado de duração de viagem para a distância escolhida.");
        }

        //graficos de linha
        ArrayList<String> labelSeries = new ArrayList<>();
        labelSeries.add("Velocidade");
        ArrayList[] data = new ArrayList[1];
        data[0] = vetorToArray(velocidadeD);

        gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha(pastaGraficos, "DIA VelMed", data, labelValues, labelSeries, "Horário (h)", "Velocidade média (kmph)");

        data = new ArrayList[1];
        data[0] = vetorToArray(distanciaD);
        labelSeries = new ArrayList<>();
        labelSeries.add("Distância");
        gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha(pastaGraficos, "DIA Dist", data, labelValues, labelSeries, "Horário (h)", "Distância (m)");

        data = new ArrayList[1];
        data[0] = vetorToArray(duracaoD);
        labelSeries = new ArrayList<>();
        labelSeries.add("Duração");
        gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha(pastaGraficos, "DIA Duração", data, labelValues, labelSeries, "Horário (h)", "Duração (min)");

        data = new ArrayList[1];
        data[0] = vetorIToArray(chamadasD);
        labelSeries = new ArrayList<>();
        labelSeries.add("Chamadas");
        gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha(pastaGraficos, "DIA Chamadas", data, labelValues, labelSeries, "Horário (h)", "Chamadas (un)");

        //para todo o periodo
        gx = new GeraGraficos(1000, 800);
        gx.timeSeriesGraficoInt(pastaGraficos, "ANO Chamadas", chamadasT, datas);

        gx = new GeraGraficos(1000, 800);
        gx.timeSeriesGrafico(pastaGraficos, "ANO Duração", duracaoT, datas);

        gx = new GeraGraficos(1000, 800);
        gx.timeSeriesGrafico(pastaGraficos, "ANO Distâncias", distanciaT, datas);

        gx = new GeraGraficos(1000, 800);
        gx.timeSeriesGrafico(pastaGraficos, "ANO Velocidade média", velocidadeT, datas);

        System.out.println("OK: Salvou resumos de dias! " + horaAtual());

    }

    public static ArrayList<Double> vetorToArray(Double[] ve) {
        ArrayList<Double> d = new ArrayList<>();
        d.addAll(Arrays.asList(ve));
        return d;
    }

    private static void calcularResumoDados2() {

        System.out.println("PROC: Calcular resumo dos dados: " + linesToRead + " (x " + taxiBatches + " = " + taxiBatches * linesToRead + ") " + horaAtual());

        String det = tempoInicio + "at" + tempoFim + "de" + discretTemporal + " wd" + weekDays + " we" + weekEnd;

        DescriptiveStatistics tempo = new DescriptiveStatistics();
        DescriptiveStatistics dist = new DescriptiveStatistics();

        ArrayList<Double> tempoV = new ArrayList<>();
        ArrayList<Double> distV = new ArrayList<>();

        ArrayList<Double> labelValues = new ArrayList<>();
        ArrayList<Double> labelSequencial = new ArrayList<>();

        ArrayList<Double> distancias = new ArrayList<>();
        ArrayList<Double> tempos = new ArrayList<>();
        ArrayList<Double> mediaVelocidade = new ArrayList<>();

        ArrayList<Double>[] percs = new ArrayList[5];
        for (int x = 0; x < 5; x++) {
            percs[x] = new ArrayList<>();
        }

        DescriptiveStatistics perc = null;

        mapa.criarROIs();
        novoOSMDat = true;
        tempoVet = new ArrayList<>();
        distVet = new ArrayList<>();
        diaVet = new ArrayList<>();
        weekdayVet = new ArrayList<>();

        createODMatrix();

        for (int bat = 0; bat < taxiBatches; bat++) {

            loadTaxiData(bat * linesToRead, linesToRead); //caso não leia, inicia processamento para gerar novo arquivo dat

            if (bat == 0) {

            }

            // if (selecionarRegistrosParaDatasetsDif) {
            clearRegistrosRuins(false, false);
            // }

            //mapTaxiTrips(false);
            for (int x = 0; x < cont_viagens_Taxi - 1; x++) {
                //if(x<2 || viagens_Taxi[x].duracaoCorridaMin() > 0.5){
                tempoV.add(viagens_Taxi[x].duracaoCorridaMin());
                distV.add(viagens_Taxi[x].distanciaDaCorridaM());
                tempo.addValue(tempoV.get(tempoV.size() - 1));
                dist.addValue(distV.get(tempoV.size() - 1));
                // }
            }

            // identificarQuadrantesOD(); //Calculos nos objetos de viagem_Taxi // acrescentarDadosDeTaxiNaODMatrix();
            //    System.out.println("OK: Fim batch " + (bat + 1) + "/" + taxiBatches + "!  " + horaAtual() + "\n");
        }

        //gerar gráfico distribuição tempo e distanacia
        String dt = "tempo, ";
        String dd = "distancia, ";
        String dm = "vel med, ";

        String tMin = "t5, ";
        String tMin2 = "t10, ";
        String tMax = "t95, ";
        String tMax2 = "t90, ";
        String tMed = "tMed, ";
        String cb = "x, ";

        Double[] tempoMin = new Double[101];
        Double[] tempoMed = new Double[101];
        Double[] tempoMax = new Double[101];
        int[] tempoCont = new int[101];
        double men = -1, mai = -1;

        for (int x = 0; x <= 100; x++) { //passando pelos percentis

            tempoMin[x] = 500.0;
            tempoMax[x] = -1.0;
            tempoMed[x] = 0.0;
            tempoCont[x] = 0;

            switch (x) {
                case 0:
                    dt = dt + tempo.getMin() + ", ";
                    dd = dd + dist.getMin() + ", ";
                    tempos.add(tempo.getMin());
                    distancias.add(dist.getMin());
                    mediaVelocidade.add(3.6 * distancias.get(distancias.size() - 1) / (tempos.get(distancias.size() - 1) * 60));
                    men = dist.getMin();
                    mai = dist.getPercentile(1);
                    break;
                case 100:
                    dt = dt + tempo.getMax() + ", ";
                    dd = dd + dist.getMax() + ", ";
                    break;
                default:
                    dt = dt + tempo.getPercentile(x) + ", ";
                    tempos.add(tempo.getPercentile(x));
                    distancias.add(dist.getPercentile(x));
                    mediaVelocidade.add(3.6 * distancias.get(distancias.size() - 1) / (tempos.get(distancias.size() - 1) * 60));
                    dd = dd + dist.getPercentile(x) + ", ";
                    dm = dm + mediaVelocidade.get(mediaVelocidade.size() - 1);
                    men = dist.getPercentile(x);
                    if (x == 99) {
                        mai = dist.getMax();
                    } else {
                        mai = dist.getPercentile(x + 1);
                    }
                    break;
            }
            cb = cb + x + ", ";

            if (x < 100) {

                labelValues.add(men);
                labelSequencial.add(x * 1.0);

                perc = new DescriptiveStatistics();
                for (int a = 0; a < tempoV.size(); a++) {

                    if (distV.get(a) >= men && distV.get(a) <= mai) {

                        perc.addValue(tempoV.get(a));
                    }

                }
            }

            //tMin = tMin + tempoMin[x] + ", ";
            tMin = tMin + perc.getPercentile(5) + ", ";
            percs[0].add(perc.getPercentile(5));

            tMin2 = tMin2 + perc.getPercentile(10) + ", ";
            percs[1].add(perc.getPercentile(10));

            tMax = tMax + perc.getPercentile(95) + ", ";
            percs[4].add(perc.getPercentile(95));

            tMax2 = tMax2 + perc.getPercentile(90) + ", ";
            percs[3].add(perc.getPercentile(90));
            //tMed = tMed + (tempoMed[x] / tempoCont[x]) + ", ";
            tMed = tMed + perc.getPercentile(50) + ", ";
            percs[2].add(perc.getPercentile(50));

        }

        ArrayList<String> labelSeries = new ArrayList<>();
        labelSeries.add("p5");
        labelSeries.add("p10");
        labelSeries.add("p50");
        labelSeries.add("p90");
        labelSeries.add("p95");

        salvarTxt(pastaPlanilhasResumos + "\\RES percents " + det + ".csv", cb + "\n" + dd + "\n" + dt + "\n" + dm + "\n\n\n" + cb + "\n" + dd + "\n" + tMin + "\n" + tMin2 + "\n" + tMed + "\n" + tMax + "\n" + tMax2 + "\n");

        GeraGraficos gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha(pastaGraficos, "TpD " + det, percs, labelValues, labelSeries, "Distância (m)", "Tempo (min)");

        //ArrayList[] percs2 = new ArrayList[1];
        //percs2[0] = distancias;
        labelSeries = new ArrayList<>();
        labelSeries.add("Distância");
        gx = new GeraGraficos(1000, 800);
        gx.GeraCDF2_From_1_DescpStats(pastaGraficos, "Dist " + det, dist, labelSeries);
        //gx.GeraGraficosLinha(pastaGraficos, "Dist " + det, percs2, labelSequencial, labelSeries, "Percentis", "Distância (m)");

        //ArrayList[] percs3 = new ArrayList[1];
        //percs3[0] = tempos;
        labelSeries = new ArrayList<>();
        labelSeries.add("Duração");
        gx = new GeraGraficos(1000, 800);
        gx.GeraCDF2_From_1_DescpStats(pastaGraficos, "Durat " + det, tempo, labelSeries);
        //gx.GeraGraficosLinha(pastaGraficos, "Durat " + det, percs3, labelSequencial, labelSeries, "Percentis", "Tempo (min)");

        mediaVelocidade = new ArrayList<>();
        labelValues = new ArrayList<>();

        ArrayList<Integer> contViagens = new ArrayList<>();
        labelValues = new ArrayList<>();
        labelValues.add(0.0);
        mediaVelocidade.add(0.0);

        int intervs = 80;
        double minD = dist.getMin(), maxD = dist.getPercentile(99);
        double step = (maxD - minD) / intervs;
        double in, fim;
        for (int x = 0; x < (intervs); x++) {

            in = minD + x * step;
            fim = minD + (x + 1) * step;
            labelValues.add((in + fim) / 2);
            contViagens.add(0);
            mediaVelocidade.add(0.0);

            //tempoV, distV
            for (int v = 0; v < tempoV.size(); v++) {
                if (distV.get(v) > in && distV.get(v) <= fim) {
                    mediaVelocidade.set(x, mediaVelocidade.get(x) + (3.6 * distV.get(v) / (tempoV.get(v) * 60)));
                    contViagens.set(x, contViagens.get(x) + 1);
                }
            }
            if (contViagens.get(x) > 0) {
                mediaVelocidade.set(x, mediaVelocidade.get(x) / contViagens.get(x));
            }
        }

        ArrayList[] percs4 = new ArrayList[1];
        percs4[0] = mediaVelocidade;
        labelSeries = new ArrayList<>();
        labelSeries.add("Média de velocidade");
        gx = new GeraGraficos(1000, 800);
        gx.GeraGraficosLinha(pastaGraficos, "VelMed " + det, percs4, labelValues, labelSeries, "Distância", "Velocidade (kmph)");

        System.out.println("Salvou RES " + det + "! " + horaAtual());

    }

    public static String gerarDataSet2(String cabe) {

        String d = cabe;

        //"dia, di5, di10, di20, dist35, dist50, di65, dit80, di90, di95, t5, t10, t20, t35, t50, t65, t80, t90, t95, weekday";
        for (int a = 0; a < distVet.size(); a++) {

            d = d + diaVet.get(a) + "," + distVet.get(a).getPercentile(5) + ", " + distVet.get(a).getPercentile(10) + ", " + distVet.get(a).getPercentile(20) + ", " + distVet.get(a).getPercentile(35) + ", " + distVet.get(a).getPercentile(50) + ", " + distVet.get(a).getPercentile(65) + ", " + distVet.get(a).getPercentile(80) + ", " + distVet.get(a).getPercentile(90) + ", " + distVet.get(a).getPercentile(95) + ", "
                    + tempoVet.get(a).getPercentile(5) + ", " + tempoVet.get(a).getPercentile(10) + ", " + tempoVet.get(a).getPercentile(20) + ", " + tempoVet.get(a).getPercentile(35) + ", " + tempoVet.get(a).getPercentile(50) + ", " + tempoVet.get(a).getPercentile(65) + ", " + tempoVet.get(a).getPercentile(80) + ", " + tempoVet.get(a).getPercentile(90) + ", " + tempoVet.get(a).getPercentile(95) + ", " + String.valueOf(weekdayVet.get(a)) + ", " + distVet.get(a).getN() + "\n";

        }

        return d;

    }

    static ArrayList<DescriptiveStatistics> distVet;
    static ArrayList<DescriptiveStatistics> tempoVet;
    static ArrayList<String> diaVet;
    static ArrayList<Character> weekdayVet;

    public static void acumularDadosDiaDataset2() {

        for (int x = 0; x < cont_viagens_Taxi; x++) {
            novoRegDia(viagens_Taxi[x].distanciaDaCorridaM(), viagens_Taxi[x].duracaoCorridaMin(), viagens_Taxi[x].getMesX31maisDia(), viagens_Taxi[x].getDay_type());
        }
    }

    public static void novoRegDia(Double distancia, Double minutos, String dia, Character weekday) {

        if (diaVet.contains(dia)) {

            distVet.get(diaVet.indexOf(dia)).addValue(distancia);
            tempoVet.get(diaVet.indexOf(dia)).addValue(minutos);

        } else {

            diaVet.add(dia);
            weekdayVet.add(weekday);
            distVet.add(new DescriptiveStatistics());
            tempoVet.add(new DescriptiveStatistics());

            distVet.get(diaVet.indexOf(dia)).addValue(distancia);
            tempoVet.get(diaVet.indexOf(dia)).addValue(minutos);

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

    public static ArrayList<Double> vetorIToArray(int[] ve) {
        ArrayList<Double> d = new ArrayList<>();
        for (int x = 0; x < ve.length; x++) {
            d.add(ve[x] * 1.0);
        }
        return d;
    }

    public static void criaDiretorio(String novoDiretorio) {

        try {
            if (!new File(novoDiretorio).exists()) { // Verifica se o diretório existe.   
                (new File(novoDiretorio)).mkdir();   // Cria o diretório   
            }
        } catch (Exception ex) {
            System.out.println("ERROR: Falha ao criar diretório " + novoDiretorio);
        }
    }

    public static boolean proximo(double raio, double latIn1, double latIn2, double lonIn1, double lonIn2,
            double latFim1, double latFim2, double lonFim1, double lonFim2) {

        if ((Math.sqrt((latIn1 - latIn2) * (latIn1 - latIn2) + (lonIn1 - lonIn2) * (lonIn1 - lonIn2)) / 0.0000089) > raio) {
            return false;
        }

        return ((Math.sqrt((latFim1 - latFim2) * (latFim1 - latFim2) + (lonFim1 - lonFim2) * (lonFim1 - lonFim2)) / 0.0000089) < raio);

    }

    private static void runODEstimation() {

        Output output = new Output();
        output.setTitle("Loading...");
        output.addText(horaAtual() + "> Iniciando componentes... ");

        if (!lerArquivoODMatrixDat() || !lerArquivoVirtualSensors(0)) {
            System.out.println("Necessário criar arquivo ODMatrix/VirtualSensors antes.");
            processarVirtSensorsEODMatrixPorBatches();//processarODTripMatrixPorBatches();
        }

        /*if (!lerArquivoVirtualSensors()) {
            System.out.println("Necessário criar arquivo virtualSensors antes.");
            processarVitualSensorsPorBatches();
        }*/
        PSO pso = new PSO(VirtualSensorsFile, discretTemporal);
        pso.setMinMaxRand(minRand, maxRand);
        pso.setOutput(output);
        pso.setTesteRedeFechada(!usandoOSMData,mapa);

        boolean testeParametros = false;
        if (testeParametros) {
            pso.setUseMatrixPriori(useMatrixPriori);
            pso.setUseVariance(false);
            pso.encontrarParametrosPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 8, 3, 8, 3, numSensores, 5);
            return;
        }

        //pso.testBatchesOptions(ODmatrix);
        boolean onlyAlgeb = false;
        boolean gerarCodigoGUSEK = true;
        boolean digerirResultsPL = false;

        if (onlyAlgeb) {

            pso.setOutputName("Solucoes algebricas em andamento...");
            pso.runAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 16, 16, numSensores, runs);
            pso.runAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 16, 14, numSensores, runs);
            pso.runAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 16, 10, numSensores, runs);
            pso.runAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 8, 8, numSensores, runs);
            pso.runAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 8, 10, numSensores, runs);
            pso.runAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 8, 14, numSensores, runs);

            //           pso.runMultipleAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 8, 8, 1000, 30000, 1000, pastaGraficos);
            pso.outputDispose();
            return;
        }
        if (gerarCodigoGUSEK) {
            pso.setOutputName("Gerador de codigos GUSEK");
            if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
                loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
            }
            
            pso.gerarCodPLGusek(numSensores, 8, 8, 0, virtualSensors, ODmatrix, mapa, "Rmais");
            pso.gerarCodPLGusek(numSensores, 8, 8, 0, virtualSensors, ODmatrix, mapa, "Rmenos");
            pso.gerarCodPLGusek(numSensores, 8, 8, 0, virtualSensors, ODmatrix, mapa, "F");
            pso.gerarCodPLGusek(numSensores, 8, 8, 0, virtualSensors, ODmatrix, mapa, "Fr");
            

            pso.outputDispose();
            return;
        }
        if (digerirResultsPL) {

            pso.setOutputName("Digerindo resultados de modelos PL GUSEK");
            pso.digerirResultadosPL("8 8 2000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 20.882065 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 296.417847 0.000000 0.000000 26.000000 0.000000 0.000000 0.000000 0.000000 461.724612 23.719232 15.000000 0.000000 0.000000 0.000000 0.000000 0.000000 37.359837 283.621793 0.000000 0.000000 0.000000 0.000000 223.342891 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 51.720709 0.000000 101.217194 0.000000 0.000000 0.000000 0.000000 9.448537 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 179.428249 0.000000 0.000000 845.225571 0.000000 213.889961 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 62.231287 0.000000 85.293525 163.550552 769.139657 33.832424 202.447886 19.163411 585.889271 153.525882 52.389236 0.000000 37.270754 0.000000 0.000000 0.000000 0.000000 81.339003 0.000000 0.000000 27.724454 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 13.021561 12.666667 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 25.333333 0.000000 0.000000 0.000000 0.000000 0.000000 421.076981 0.000000 0.000000 0.000000 0.000000 20.271890 290.595343 425.483030 41.408185 0.000000 202.325052 237.615828 0.000000 96.593422 92.926471 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 38.000000 1399.359764 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 22.137011 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 107.412027 0.000000 38.469189 0.000000 46.019924 21.244279 0.000000 1902.437775 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 2.435848 0.000000 0.000000 0.000000 0.000000 0.000000 8.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000",
                    virtualSensors, ODmatrix, runs);
            /*pso.digerirResultadosPL("8 10 7500 58.943743 0.000000 30.096938 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 1.208622 0.000000 0.000000 11.012493 0.000000 0.000000 0.000000 0.000000 0.000000 88.425940 104.828968 0.000000 42.831639 0.000000 0.000000 0.000000 0.000000 1.988887 0.000000 0.000000 0.000000 0.000000 2.936764 0.000000 0.000000 0.000000 43.075136 190.077627 0.000000 31.963462 0.000000 225.414683 0.000000 0.178711 0.215120 0.000000 0.000000 96.104870 291.085851 18.391583 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 318.026747 135.000000 41.647320 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 423.000000 5.277263 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 26.889688 937.500000 70.731174 0.000000 47.475293 0.000000 29.412249 0.000000 187.811390 136.451062 38.323059 0.000000 0.000000 107.493793 204.022132 0.000000 0.000000 96.308478 280.073251 3310.778778 96.000000 0.000000 0.000000 2175.967237 1030.500000 64.616555 157.174670 0.000000 0.000000 0.000000 0.000000 0.000000 10.099967 0.000000 0.000000 28.704929 0.000000 3096.000000 0.000000 0.000000 458.837306 1664.163517 0.000000 0.000000 24.508591 0.000000 0.000000 0.000000 5.780397 364.969851 0.000000 0.000000 0.000000 16.839150 0.000000 0.000000 12.378836 0.000000 154.399959 0.000000 0.000000 21.136561 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 6.373195 0.000000 0.000000 0.000000 8.375046 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 66.143600 47.753847 0.000000 0.000000 58.640182 216.414902 56.000000 0.000000 0.000000 778.777044 163.822812 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 31.437549 0.000000 519.986273 78.178692 0.000000 0.000000 2151.000000 151.250856 42.139178 0.000000 0.000000 0.000000 0.000000 20.237114 85.999928 0.000000 0.000000 25.015499 0.000000 25.855671 86.349755 0.000000 196.251479 0.000000 335.366246 0.000000 0.000000 0.000000 0.000000 0.000000 1.004702 123.400031 0.000000 163.678214 478.496905 37.193575 0.000000 158.895498 0.000000 0.000000 5.021964 0.000000 813.883758 5.380908 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 ",
                     virtualSensors, ODmatrix);
            pso.digerirResultadosPL("8 14 7500 22.451287 0.000000 5.283290 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 189.994903 69.242284 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 49.906361 2.417940 0.000000 0.000000 0.000000 156.900050 229.978151 0.000000 35.965038 0.000000 172.714085 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 21.942244 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 62.457647 126.697682 0.000000 2.938841 0.000000 205.365302 0.000000 0.000000 3.710025 76.689020 0.000000 0.000000 0.000000 0.000000 440.779606 0.000000 0.000000 236.310913 375.000000 0.000000 0.000000 10.380877 0.000000 96.263656 0.000000 0.000000 35.094588 0.000000 0.000000 0.000000 62.569226 54.762010 0.000000 0.000000 135.650012 519.460497 6015.000000 117.547795 0.000000 0.000000 1971.000000 270.990242 216.748642 378.361209 0.000000 0.000000 0.000000 0.000000 0.000000 12.385344 0.000000 0.000000 21.715517 107.615102 1769.283662 0.000000 0.000000 104.983799 2223.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 51.688559 0.000000 0.000000 0.000000 37.688124 0.000000 0.000000 0.000000 0.000000 118.648305 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 6.934502 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 43.212213 0.000000 0.000000 69.055969 252.743583 351.938876 0.000000 0.000000 412.249837 194.280898 251.782029 0.000000 31.804406 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 39.618326 30.305262 0.000000 749.758361 175.000000 0.000000 451.519771 1365.975156 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 62.330129 0.000000 15.059613 0.000000 12.083087 0.000000 79.113875 7.285677 0.000000 0.000000 220.196801 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 528.000000 382.744178 3.134573 0.000000 131.596208 0.000000 63.057328 0.000000 0.000000 255.989614 4.957101 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 3.963694 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 ",
                     virtualSensors, ODmatrix);

            pso.digerirResultadosPL("16 10 7500 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 45.000000 0.000000 420.000000 0.000000 25.545873 0.000000 0.000000 0.000000 0.000000 26.666667 0.000000 199.967128 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.602302 0.000000 0.000000 0.000000 111.769205 87.059106 0.000000 0.000000 5.489318 0.000000 78.037229 0.000000 365.839642 3.167357 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 4.406760 3.806973 0.000000 0.000000 288.750000 87.755901 12.514577 0.722665 0.000000 0.000000 0.000000 0.000000 225.000000 0.921517 0.000000 0.000000 0.000000 0.000000 59.834648 0.000000 325.712567 260.774850 496.046649 0.000000 309.214158 36.899628 132.219008 0.000000 0.000000 133.493052 10.000000 0.000000 0.000000 0.000000 12.058282 21.894199 0.000000 0.000000 736.841954 1560.374825 43.230488 122.016459 0.000000 4296.000000 0.000000 78.033692 0.000000 85.500000 0.000000 0.000000 0.000000 0.000000 52.000000 0.000000 5.971526 4.140332 13.162545 1324.716957 0.000000 0.000000 252.526657 97.682258 119.943731 45.035848 0.000000 0.000000 0.000000 13.921548 108.000000 150.000000 0.000000 0.000000 0.000000 64.152897 17.998042 80.000000 0.000000 0.000000 31.493736 0.000000 0.000000 13.189937 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 1.021441 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 8.861129 0.000000 0.000000 129.096729 635.451820 307.764152 0.000000 10.597612 759.392209 433.162111 195.385314 84.302021 0.000000 0.000000 0.000000 0.000000 0.000000 192.230514 0.000000 19.022939 43.404254 0.000000 443.197281 0.000000 0.000000 0.000000 4347.000000 376.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 35.504016 28.262649 75.000000 0.000000 239.399006 229.348912 17.499470 0.000000 0.000000 0.000000 0.000000 0.000000 151.086598 0.000000 0.000000 121.728494 349.623468 0.000000 5.935678 38.981324 91.840439 69.289541 28.502275 41.109171 721.374597 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 41.913647 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000",
                     virtualSensors, ODmatrix);
            pso.digerirResultadosPL("16 14 7500 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 18.000000 0.000000 133.271608 0.000000 0.000000 0.000000 4.523113 1.079811 0.000000 0.000000 5.192240 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 29.470808 0.000000 0.000000 0.000000 0.000000 39.591946 0.000000 5.789877 41.521134 399.314824 0.000000 10.943804 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 4.685911 0.000000 0.000000 361.131524 236.250000 0.000000 0.000000 28.305614 42.750000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 229.846861 0.000000 0.000000 239.894664 28.210402 0.000000 0.000000 0.000000 175.000000 253.168340 0.000000 240.000000 40.798111 0.000000 0.000000 0.000000 0.000000 243.337896 0.000000 0.000000 247.888289 9774.000000 14.333333 34.613300 0.000000 455.636364 537.418207 156.551690 93.835655 49.564887 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 17.155096 19.664724 1540.000000 28.720839 0.000000 260.487273 19.400000 217.600000 18.241678 0.000000 0.000000 0.000000 2.240685 42.000000 0.000000 0.000000 0.000000 0.000000 74.430364 51.540737 0.000000 0.000000 8.486246 79.377789 0.000000 31.153439 0.260417 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 121.531736 356.283815 217.361153 0.000000 0.000000 8.782227 1039.724201 572.619683 193.213304 25.998626 0.000000 0.000000 0.000000 0.000000 0.000000 110.723023 0.000000 0.000000 43.266573 0.000000 1568.000000 0.000000 0.000000 199.561261 3139.500000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.476597 0.000000 0.000000 0.000000 39.315802 0.000000 142.060036 0.000000 34.718852 0.000000 2.777780 60.000000 0.000000 0.000000 0.000000 0.000000 0.000000 65.558836 0.000000 980.000000 105.774219 14.738814 0.000000 70.202065 0.000000 9.112150 0.000000 12.190148 246.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 17.725141 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000",
                     virtualSensors, ODmatrix);
            pso.digerirResultadosPL("16 16 7500 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 9.000000 22.000000 46.040951 0.000000 16.193417 3.956774 1.569149 0.000000 40.000000 25.000000 1.810153 35.292097 0.000000 0.000000 1.500000 0.000000 0.000000 3.821166 33.167176 31.664221 0.000000 10.721621 34.534064 72.232819 13.637676 0.000000 0.000000 31.785827 97.508348 5.683411 11.808828 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 2.203616 16.868136 0.000000 104.673356 210.000000 0.000000 0.000000 23.342698 13.890648 0.077417 0.000000 0.000000 4.705357 0.000000 0.000000 0.000000 0.000000 0.000000 8.153466 0.000000 15.269869 246.156394 178.123646 0.000000 133.388874 0.000000 0.000000 36.826828 56.175602 163.511493 62.840433 0.000000 0.000000 0.000000 15.244671 185.696496 0.000000 97.952428 323.653200 1291.392323 43.443775 33.973266 0.000000 781.429989 342.968002 22.323325 13.263046 50.621488 0.000000 0.000000 0.000000 0.492941 22.875087 0.000000 0.977630 5.826978 22.381453 660.000000 6.007201 0.000000 115.774774 196.169867 7.714454 0.000000 15.703157 0.000000 0.000000 2.018434 6.000000 23.384030 0.000000 0.000000 0.000000 2.586658 8.196014 0.000000 0.000000 7.909248 13.170107 0.000000 10.928983 1.640552 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 2.459001 0.000000 70.751146 0.000000 13.730097 5.172708 135.957751 42.324502 4.194278 2.048107 428.142067 204.858309 112.953159 26.093158 0.000000 0.000000 0.000000 0.000000 0.000000 32.392134 0.000000 9.045688 0.000000 66.605030 444.971016 0.000000 0.000000 440.117351 514.281202 0.000000 32.956424 11.558346 0.000000 0.000000 0.000000 0.000000 38.600551 0.000000 0.211101 0.000000 16.733009 0.000000 16.392275 0.000000 36.961546 46.611652 133.123131 44.460403 0.000000 0.000000 0.000000 0.000000 3.400164 86.226905 0.000000 190.076609 89.253286 33.277764 4.046423 30.654615 0.000000 0.000000 18.898633 38.436781 182.416788 6.585575 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 7.272907 0.000000 0.000000 0.000000 0.000000 5.672289 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000 0.000000",
                     virtualSensors, ODmatrix); */

            pso.outputDispose();
            return;
        }

        ArrayList<Integer> hors = new ArrayList<>();
        hors.add(10);
        hors.add(14);

        for (int h = 0; h < 1; h++) {
            if (h == 0) {
                pso.setUseMatrixPriori(false);
                pso.setUseVariance(false);
            } else if (h == 1) {
                pso.setUseMatrixPriori(true);
                pso.setUseVariance(true);
            } else {
                pso.setUseMatrixPriori(true);
                pso.setUseVariance(false);
            }

            boolean thread1 = !true;
            boolean thread2 = !true;
            boolean thread3 = !true;
            boolean thread4 = !true;
            boolean thread5 = !true;
            boolean thread6 = !true;

            if (thread1) {
                // pso.setUseMatrixPriori(true);
                //pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                //        8, 8, numSensores, runs);  //int tempoPriori, int batchPriori, int tempoProblema, int batchProblema

                pso.runGA(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 8, 0, 8, 0, numSensores, runs);
            }
            if (thread4) {
                // pso.setUseMatrixPriori(true);
                pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                        8, 8, numSensores, runs);  //int tempoPriori, int batchPriori, int tempoProblema, int batchProblema
            }

            /*if (thread1) {
            pso.runGA(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 7, 5, 7, 6, numSensores, runs);
        }*/
 /*if (thread3) {
            pso.runGA(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 7, 5, 7, 6, numSensores, runs);
        }*/
            if (thread2) {

                pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                        8, 10, numSensores, runs);
                // pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                //        16, 10,  numSensores, runs);
            }
            if (thread5) {
                pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                        16, 10, numSensores, runs);
            }

            /*if (thread2) {
            pso.runGA(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), 7, 5, 7, 6, numSensores, runs);
        }*/
            if (thread3) {
                pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                        8, 14, numSensores, runs);

            }
            if (thread6) {
                pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                        16, 14, numSensores, runs);
            }

        } // fim for horarios

        pso.outputDispose();

    }

    public static ArrayList<String> lerTxt(String file) {

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

    private static void criarCODGusek(int hora, int numSensores1, String tipoEscolhaSensores, boolean correlacSens) {
        //  Output output = new Output();
        // output.setTitle("Gerador de codigos GUSEK");
        boolean porFluxos, maisRotas;
        switch (tipoEscolhaSensores) {
            case "F":
                porFluxos = true;
                maisRotas = false;
                break;
            case "Rmais":
                porFluxos = false;
                maisRotas = true;
                break;
            default:
                porFluxos = false;
                maisRotas = false;
                break;
        }
        
        if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }
        if (!lerArquivoODMatrixDat() || !lerArquivoVirtualSensors(0)) {
            System.out.println("Necessário criar arquivo ODMatrix/VirtualSensors antes.");
            processarVirtSensorsEODMatrixPorBatches();//processarODTripMatrixPorBatches();
        }

        PSO pso = new PSO(VirtualSensorsFile, discretTemporal);
        pso.setMinMaxRand(minRand, maxRand);
        pso.setUseMatrixPriori(false);
        pso.setUseVariance(false);
        pso.setSensoresFluxoNotRotas(porFluxos, maisRotas,correlacSens,OSMFileLocation);
        // pso.setOutput(output);
        pso.setTesteRedeFechada(!usandoOSMData,mapa);

        //pso.setOutputName("Gerador de codigos GUSEK");
        pso.gerarCodPLGusek(numSensores1, hora, hora, 0, virtualSensors, ODmatrix, mapa, tipoEscolhaSensores);
    }

    private static void runODEstimationNumSensores(int hora, int inicio, int step, int fim, int fator) {

        /*if (!lerArquivoOSMDat()) { //Tentar ler arquivo pronto
            loadOSMData(); //caso não leia, inicia processamento para gerar novo arquivo dat
        }*/
        if (!lerArquivoODMatrixDat() || !lerArquivoVirtualSensors(0)) {
            System.out.println("Necessário criar arquivo ODMatrix/VirtualSensors antes.");
            processarVirtSensorsEODMatrixPorBatches();//processarODTripMatrixPorBatches();
        }

        String trabalhosFile = "OD Estimation Works.txt";
        ArrayList<String> trabs = lerTxt(trabalhosFile);
        String aux = "";
        String tipoEscolhaSensores = "Rmais"; // F, Rmais ou Rmenos   + C
        String eqFitness = "geh"; //folga, reg ou geh
        
        //criar arquivo de trabalhos, se vazio ou nao encontrado
        if (trabs.size() < 1) {
            System.out.println("PROC: Sem trabalhos em '" + trabalhosFile + "'. Cadastrando novos...");
            //gerar codigos PL
            for (int s = inicio; s < fim; s = s + step) {
                
                //criarCODGusek(hora, s, tipoEscolhaSensores); 
                trabs.add(hora + " " + s + " " + true + " " + false + " " + false + " " + false + " " + tipoEscolhaSensores+ " "+ eqFitness); //ALGEB
                trabs.add(hora + " " + s + " " + false + " " + true + " " + false + " " + false + " " + tipoEscolhaSensores+ " "+ eqFitness); //PSO
                //trabs.add(hora + " " + s + " " + false + " " + true + " " + false + " " + false + " " + tipoEscolhaSensores+ " "+ "folga"); //PSO
                //trabs.add(hora + " " + s + " " + false + " " + true + " " + false + " " + false + " " + tipoEscolhaSensores+ " "+ "reg"); //PSO
                trabs.add(hora + " " + s + " " + false + " " + false + " " + true + " " + false + " " + tipoEscolhaSensores+ " "+ eqFitness); //GA
                trabs.add(hora + " " + s + " " + false + " " + false + " " + false + " " + true + " " + tipoEscolhaSensores+ " "+ eqFitness); //Dig. PL
            }

            for (int t = 0; t < trabs.size(); t++) {
                aux = aux + trabs.get(t) + "\n";
            }

            salvarTxt(trabalhosFile, aux);

        }

        //le o arquivo de trabalhos, mesmo que tenha criado um novo (outra thread pode ter sido mais rapida e ja pego um trabalho)
        trabs = lerTxt(trabalhosFile);

        //enquanto houverem trabalhos disponiveis, vai para o proximo
        while (trabs.size() > 0) {
            System.out.println("PROC: Iniciando trabalhos... " + trabs.size() + " trabalhos na fila. Atual: " + trabs.get(0));
            //identifica trabalho
            int h = Integer.valueOf(trabs.get(0).split(" ")[0]);
            int ns = Integer.valueOf(trabs.get(0).split(" ")[1]);
            boolean algeb = Boolean.valueOf(trabs.get(0).split(" ")[2]);
            boolean psx = Boolean.valueOf(trabs.get(0).split(" ")[3]);
            boolean gax = Boolean.valueOf(trabs.get(0).split(" ")[4]);
            boolean digPl = Boolean.valueOf(trabs.get(0).split(" ")[5]);
            tipoEscolhaSensores = trabs.get(0).split(" ")[6];
            eqFitness = trabs.get(0).split(" ")[7];
            boolean correlacSens=false;

            boolean porFluxos, maisRotas;
            if(tipoEscolhaSensores.equals("F") || tipoEscolhaSensores.equals("FC")){
                porFluxos = true;
                maisRotas = false;
                if(tipoEscolhaSensores.equals("FC"))
                    correlacSens = true;
                //ns++;
            }else if(tipoEscolhaSensores.equals("Rmais") || tipoEscolhaSensores.equals("RmaisC")){
                porFluxos = false;
                maisRotas = true;
                if(tipoEscolhaSensores.equals("RmaisC"))
                    correlacSens = true;
                    
            }else{
                porFluxos = false;
                maisRotas = false;
                ///ns--;
                if(tipoEscolhaSensores.equals("RmenosC"))
                    correlacSens = true;
            }
               
            
            //remove trabalho da fila, salva arquivo
            aux = "";
            for (int t = 1; t < trabs.size(); t++) {
                aux = aux + trabs.get(t) + "\n";
            }
            salvarTxt(trabalhosFile, aux);

            //roda trabalho
            runODEstimationResumo(h, ns, algeb, gax, psx, false, digPl, porFluxos, maisRotas, correlacSens, fator, eqFitness);
            //procura novo trabalho
            trabs = lerTxt(trabalhosFile);

        }
        
        System.out.println("\nOK: Lista de trabalhos concluida!");

    }

    private static void runODEstimationResumo(int hora, int numSensores, boolean algeb, boolean runGA, boolean runPSO, boolean gerarCodGUSEK, boolean digerirPL, 
            boolean porFluxos, boolean maisRotas, boolean  correlacSens,int fator, String eqFitness) {

        if (!lerArquivoODMatrixDat() || !lerArquivoVirtualSensors(0)) {
            System.out.println("Necessário criar arquivo ODMatrix/VirtualSensors antes.");
            processarVirtSensorsEODMatrixPorBatches();//processarODTripMatrixPorBatches();
        }

        Output output = new Output();
        System.out.println("OK: Iniciando runODEstimationResumo(hora=" + hora + ", numSensores=" + numSensores + ")");
        output.setTitle("Loading...");
        output.addText(horaAtual() + "> Iniciando componentes... ");

        if (runGA) {
            System.out.println("GA ativo!");
        }
        if (runPSO) {
            System.out.println("PSO ativo!");
        }

        PSO pso = new PSO(VirtualSensorsFile, discretTemporal);
        pso.setMinMaxRand(minRand, maxRand);
        pso.setUseMatrixPriori(useMatrixPriori);
        pso.setUseVariance(false);
        pso.setOutput(output);
        pso.setTesteRedeFechada(!usandoOSMData,mapa);
        pso.setSensoresFluxoNotRotas(porFluxos, maisRotas,correlacSens,OSMFileLocation);

        if (gerarCodGUSEK) {
            
           String tipoNSLP;
            if (porFluxos) {
                if(maisRotas)
                tipoNSLP =  "_F";
                else
                    tipoNSLP = "_Fr";
            } else {
                if(maisRotas)
                    tipoNSLP = "_Rmais";
                else
                    tipoNSLP = "_Rmenos";
            }
            
            pso.gerarCodPLGusek(numSensores, hora, hora, 0, virtualSensors, ODmatrix, mapa, tipoNSLP);
        }

        pso = new PSO(VirtualSensorsFile, discretTemporal);
        pso.setMinMaxRand(minRand, maxRand);
        pso.setUseMatrixPriori(useMatrixPriori);
        pso.setUseVariance(false);
        pso.setOutput(output);
        pso.setTesteRedeFechada(!usandoOSMData,mapa);
        pso.setSensoresFluxoNotRotas(porFluxos, maisRotas,correlacSens,OSMFileLocation);
        pso.setOutputName("Solucao algebrica:");
        if (algeb) {
            pso.runAlgebricSolution(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), hora, hora, numSensores, runs);
        }

        pso = new PSO(VirtualSensorsFile, discretTemporal);
        pso.setMinMaxRand(minRand, maxRand);
        pso.setUseMatrixPriori(useMatrixPriori);
        pso.setUseVariance(false);
        pso.setOutput(output);
        pso.setTesteRedeFechada(!usandoOSMData,mapa);
        pso.setSensoresFluxoNotRotas(porFluxos, maisRotas,correlacSens,OSMFileLocation);
        pso.setFuncaoFitness(eqFitness);

        if (runPSO) {

            
            pso.setParamPSO(s_pso * fator, it_pso * fator , wIn, wF, c1, c2, it_reset_pso, 0.9);
            //(int numP, int iterat, double wIn, double wF, double c1, double c2)
            pso.runPSO(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(),
                    hora, hora, numSensores, runs);  //int tempoPriori, int batchPriori, int tempoProblema, int batchProblema

        }

        if (digerirPL) {
            pso.setOutputName("Digerindo resultados de modelos PL GUSEK");
            
            String tipoNSLP;
            if (porFluxos) {
                if(maisRotas)
                tipoNSLP =  "F";
                else
                    tipoNSLP = "Fr";
            } else {
                if(maisRotas)
                    tipoNSLP = "Rmais";
                else
                    tipoNSLP = "Rmenos";
            }
            
            
            pso.digerirPLCadastrado(hora, numSensores, tipoNSLP, virtualSensors, ODmatrix, runs);
        }

        if (runGA) {
            pso.setParamGA(pop * fator, geracoes * fator, crossover, mutacao, varMut); //(int populacao, int geracoes, double crossover, double mutacao, double varMut)
            pso.runGA(ODmatrix, virtualSensors, ODmatrix.getNumeroClusters(), hora, 0, hora, 0, numSensores, runs);
        }

        pso.outputDispose();

    }

}
