/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package taxi.od.solver;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * @author Luciano
 */
public class taxi_Trip_Instance implements Serializable {

    // Taxi Data: https://archive.ics.uci.edu/ml/datasets/Taxi+Service+Trajectory+-+Prediction+Challenge,+ECML+PKDD+2015#
    // TRIP_ID(0), CALL_TYPE(1), ORIGIN_CALL(2), ORIGIN_STAND(3), TAXI_ID(4), TIMESTAMP(5), DAY_TYPE(6), MISSING_DATA(7), POLYLINE(8)
    // 1372636858620000589, C,        ,             , 2000589, 1372636358,      A,         False, [[-8.618643, 41.141412], [-8.618649, 41.141376]]
    private Double[] lat;
    private Double[] lon;
    private short cont_pos;

    // private final String trip_id;
    private final Character call_type;
    private final Character day_type;
    private final Character missing_data;
    private boolean semRegistros = false;

    private final Timestamp hora_inicio;

    //nodes e ways pelos quais passa
    //private ArrayList<String> nodes;
    private ArrayList<Integer> posNodesEmMapa;
    //private ArrayList<String> ways;
    //private ArrayList<Integer> waysEmMapa;

    private short quadrantePartidaLat;
    private short quadrantePartidaLon;
    private short quadranteChegadaLat;
    private short quadranteChegadaLon;

    private final byte metrosRaioNode;
    private final byte registrosPorMinuto;

    // private Mapping tempMap = null;
    private final String taxi_id;

    private byte[][] bloc;
    private Mapping tempMap = null;

    public int getHoraInt() {
        return hora_inicio.getHours();
    }

    public int getMais30min() {

        if (hora_inicio.getMinutes() >= 30) {
            return 1;
        } else {
            return 0;
        }
    }

    public void encontrarBlocosQuePassa(double latIn, double lonIn, double tamBlocoDeg, int ROInumberLat, int ROInumberLon) {

        int iLat, iLon;
        bloc = new byte[ROInumberLat][ROInumberLon];

        for (int la = 0; la < ROInumberLat; la++) {
            for (int lo = 0; lo < ROInumberLon; lo++) {
                bloc[la][lo] = 0;
            }
        }

        for (int c = 0; c < getCont_pos(); c++) {

            //encontrar index Lat e Lon
            iLat = (int) (((lat[c] - latIn) / tamBlocoDeg) - 0.5);
            iLon = (int) (((lon[c] - lonIn) / tamBlocoDeg) - 0.5);

            // if(iLon<0)
            //    iLon = -iLon;
            //setar
            if (iLat >= 0 && iLon >= 0 && iLat < ROInumberLat && iLon < ROInumberLon) {
                bloc[iLat][iLon] = 1;
            }
            //  else
            //   System.out.println("ER: iLat="+iLat+"; iLon="+iLon+" (max "+ROInumberLat+", "+ROInumberLon+").(pos "+lat[c]+", "+lon[c]+") (ini "+latIn+", "+lonIn+")");

        }

    }

    public String getInstanciaPassaBlocos(double latIn, double lonIn, double tamBlocoDeg, int ROInumberLat, int ROInumberLon) {

        encontrarBlocosQuePassa(latIn, lonIn, tamBlocoDeg, ROInumberLat, ROInumberLon);

        //if(bloc==null)
        //    System.out.println("ERROR: Buscou blocos que passou sem antes ter usado encontrarBlocosQuePassa()");
        String s = "";

        for (int la = 0; la < ROInumberLat; la++) {
            for (int lo = 0; lo < ROInumberLon; lo++) {
                s = s + bloc[la][lo] + ", ";
            }
        }

        s = s + this.duracaoCorridaMin() + "\n";

        return s;
    }

    public String getCabecPassaBlocos(int ROInumberLat, int ROInumberLon) {

        String s = "";

        for (int la = 0; la < ROInumberLat; la++) {
            for (int lo = 0; lo < ROInumberLon; lo++) {
                s = s + "Bla" + la + "lo" + lo + ", ";
            }
        }

        s = s + "duracao";

        return s;
    }

    public void addPontosLatLon(ArrayList<Double> lats, ArrayList<Double> lons) {
        lat = new Double[lats.size()];
        lon = new Double[lats.size()];

        for (int x = 0; x < lats.size(); x++) {
            lat[x] = lats.get(x);
            lon[x] = lons.get(x);
        }
        cont_pos = (short) lats.size();

    }

    public String demonstrarCaminhoEmMapa(Mapping map) {

        String ponts = "http://www.darrinward.com/lat-long/\n\n";//"Viagem " + v + " Pontos: " + ponts + "\n";

        for (int x = 0; x < getCont_pos(); x++) {
            ponts = ponts + lat[x] + ", " + lon[x] + "\n";
        }

        return ponts;
    }

    public String demonstrarNosEmMapa(Mapping map) {

        String ponts = "http://www.darrinward.com/lat-long/";//"Viagem " + v + " Pontos: " + ponts + "\n";
        double lat, lon;

        for (int x = 0; x < posNodesEmMapa.size(); x++) {
            lat = map.getNode_lat()[posNodesEmMapa.get(x)];
            lon = map.getNode_lon()[posNodesEmMapa.get(x)];
            ponts = ponts + lat + ", " + lon + "\n";

            if (x < posNodesEmMapa.size() - 1) {
                double difLat, difLon;
                difLat = map.getNode_lat()[posNodesEmMapa.get(x + 1)] - lat;
                difLon = map.getNode_lon()[posNodesEmMapa.get(x + 1)] - lon;

                for (int p = 0; p < 4; p++) {
                    ponts = ponts + (lat + (difLat * p / 4)) + ", " + (lon + (difLon * p / 4)) + "\n";
                }

            }

        }

        return ponts.substring(0, ponts.length() - 2);
    }

    public String instanciaDatasetODTempo() {   //detecção de anomalias em viagem
        //latIn, lonIn, latF, lonF, tempoIn, duration, weekday, taxi_id

        if (getCont_pos() < 1) {
            return "";
        }

        return lat[0] + ", " + lon[0] + ", " + lat[getCont_pos() - 1] + ", " + lon[getCont_pos() - 1] + ", "
                + "" + (hora_inicio.getHours() * 60 + hora_inicio.getMinutes()) + ", " + (this.duracaoCorridaMin()) + ", " + getDay_type() + ", t" + this.getTaxi_id() + "\n";

    }

    public String instanciaLocalizacao() {

        if (getCont_pos() < 1) {
            return "";
        }

        return lat[0] + ", " + lon[0] + ", orig \n "
                + lat[getCont_pos() - 1] + ", " + lon[getCont_pos() - 1] + ", dest \n";

    }

    public String getMesX31maisDia() {

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(hora_inicio.getTime());

        String x = (new SimpleDateFormat("dd/MM/yyyy").format(start.getTime()));

        return x;

        //return hora_inicio.getYear() + hora_inicio.getMonth() * 31 + hora_inicio.getDay(); //retorna entre 32 e 403 + ano - 1900 =  entre 147 e 518
    }

    public void calculaQuadrantesODMatrix(double startLat, double startLon, int tamanhoGrid, int ROInmbLat, int ROInmbLon) {

        double gridEmGraus = ((double) tamanhoGrid) * 0.0000089;

        quadrantePartidaLat = (short) (((getLat()[0] - startLat) / gridEmGraus) - 0.5);
        quadrantePartidaLon = (short) (((getLon()[0] - startLon) / gridEmGraus) - 0.5);

        quadranteChegadaLat = (short) (((getLat()[getCont_pos() - 1] - startLat) / gridEmGraus) - 0.5);
        quadranteChegadaLon = (short) (((getLon()[getCont_pos() - 1] - startLon) / gridEmGraus) - 0.5);

        if (quadrantePartidaLat >= ROInmbLat) {
            quadrantePartidaLat = -1;
        }
        if (quadranteChegadaLat >= ROInmbLat) {
            quadrantePartidaLat = -1;
        }

        if (quadrantePartidaLon >= ROInmbLon) {
            quadrantePartidaLon = -1;
        }
        if (quadranteChegadaLon >= ROInmbLon) {
            quadranteChegadaLon = -1;
        }

        // System.out.println("Quads "+quadrantePartidaLat+" "+ quadrantePartidaLon+" "+ quadranteChegadaLat+ " "+ quadranteChegadaLon);
    }

    public int getNumeroArestasConectadas() {
        return conectados;
    }

    //ArrayList<Integer> fromNod;
    //ArrayList<Integer> toNod;
    //ArrayList<String> fromNodStr;
    //ArrayList<String> toNodStr;
    private int necessidadesInclude = 0;
    private int includesEfetivados = 0;
    private int conectados;
    boolean usarGemeos;

    public int construirCaminhosIncompletos(Mapping map1, boolean usarGemeos1, boolean pruning, boolean ciclo, boolean restricoesXMLSumo, int maxSaltos1) {

        maxSaltos = maxSaltos1/4;
        tempMap = map1;
        usarGemeos = usarGemeos1;
        map1 = null;
        eliminarNodesRepetidos(tempMap);
        nosIniciais = posNodesEmMapa.size();
        
        if(nosIniciais>0 && ciclo)
        posNodesEmMapa.add(posNodesEmMapa.get(0));

        int repeat = 2;
        ArrayList<Integer> novasPos;
        boolean con;

        //de node cadastrado em node cadastrado
        for (int n = 0; n < posNodesEmMapa.size() - 1; n++) {

            
            
            if (n == 0 && pruning/* && repeat == 1*/) {
                novasPos = new ArrayList<>();
                novasPos.add(posNodesEmMapa.get(0));

                for (int x = 1; x < (posNodesEmMapa.size() - 1); x++) {
                    if (tempMap.existeVizinho(posNodesEmMapa.get(x), posNodesEmMapa.get(x + 1), usarGemeos)) {
                        if (x > (posNodesEmMapa.size() - 3)) //se está no fim, add direto
                        {
                            novasPos.add(posNodesEmMapa.get(x));
                        } else if (posNodesEmMapa.get(x-1) != posNodesEmMapa.get(x + 1)){ //elimina casos de ida e volta em arestas
                        
                            novasPos.add(posNodesEmMapa.get(x));
                        } else {
                            x++;
                        }
                    }
                }
                novasPos.add(posNodesEmMapa.get(posNodesEmMapa.size() - 1));

                posNodesEmMapa = new ArrayList<>();
                for (int x = 0; x < novasPos.size(); x++) {
                    posNodesEmMapa.add(novasPos.get(x));
                }

                /*novasPos = new ArrayList<>();
                novasPos.add(posNodesEmMapa.get(0));

                for (int x = 1; x < (posNodesEmMapa.size() - 1); x++) {
                    if (tempMap.existeVizinho(posNodesEmMapa.get(x), posNodesEmMapa.get(x + 1), usarGemeos)) {
                        if (posNodesEmMapa.get(x) != posNodesEmMapa.get(x + 2)) //elemina casos de ida e volta em arestas
                        {
                            novasPos.add(posNodesEmMapa.get(x));
                        } else {
                            x++;
                        }
                    } else if (x > (posNodesEmMapa.size() - 3)) //se está no fim, add direto
                    {
                        novasPos.add(posNodesEmMapa.get(x));
                    } else {

                        if (posNodesEmMapa.get(x) != posNodesEmMapa.get(x + 2)) //elemina casos de ida e volta em arestas
                        {
                            novasPos.add(posNodesEmMapa.get(x));
                        } else {
                            x++;
                        }

                    }
                }
                novasPos.add(posNodesEmMapa.get(posNodesEmMapa.size() - 1));

                posNodesEmMapa = new ArrayList<>();
                for (int x = 0; x < novasPos.size(); x++) {
                    posNodesEmMapa.add(novasPos.get(x));
                }*/
                //System.out.println("PRUNING: De " + nosIniciais + " para " + posNodesEmMapa.size() + " nós;");
                pruning = false;
            }
            
            if(n==0 || !restricoesXMLSumo)
                con = tempMap.existeVizinho(posNodesEmMapa.get(n), posNodesEmMapa.get(n+1), false);
            else
                con = tempMap.existeVizinhoPermitido(posNodesEmMapa.get(n), posNodesEmMapa.get(n+1), tempMap.getArestaIndex(posNodesEmMapa.get(n-1), posNodesEmMapa.get(n)));
            //con = tempMap.existeVizinho(posNodesEmMapa.get(n), posNodesEmMapa.get(n+1), usarGemeos);
            //con = tempMap.existeAresta(posNodesEmMapa.get(n), posNodesEmMapa.get(n + 1));

            //caso não seja uma conexão direta
            if (!con) {

                //encontrar conexão
                novasPos = new ArrayList<>();

                for (int n2 = 0; n2 <= n; n2++) //adiciona anteriores aos nós analisados
                {
                    novasPos.add(posNodesEmMapa.get(n2));
                }

                //busca caminhos >> PARTE PRINCIPAL <<
                preparaBusca(tempMap.getContNodes());
                if(n>1  && restricoesXMLSumo)//se não tiver restrições XMLSumo, não interessa saber qual a aresta anterior
                    searchWayDistancias(posNodesEmMapa.get(n + 1), posNodesEmMapa.get(n), tempMap.getArestaIndex(posNodesEmMapa.get(n-1), posNodesEmMapa.get(n)), new int[maxSaltos + 1], 0, 0.0);
                else
                    searchWayDistancias(posNodesEmMapa.get(n + 1), posNodesEmMapa.get(n), -1, new int[maxSaltos + 1], 0, 0.0);
                //searchWaySaltos(posNodesEmMapa.get(n + 1), posNodesEmMapa.get(n), new int[maxSaltos + 1], 0);

                for (int t = 1; t < caminhoAuxTam; t++) {
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
                posNodesEmMapa.add(novasPos.get(novasPos.size() - 1)); //add ultimo ponto

                n = n + caminhoAuxTam;

            }

            if (n == (posNodesEmMapa.size() - 2) && repeat > 0) {
                n = 0;
                repeat--;
                maxSaltos = maxSaltos*2;
            }
        }

        eliminarNodesRepetidos(tempMap);
        tempMap = null;
        nos = null;
        caminhoAux = null;
        System.gc();
        return posNodesEmMapa.size();
    }

    public String eliminarNodesRepetidos(Mapping mape) {
        //eliminar nodes repetidos da trip
        String s="";
        ArrayList<Integer> aux2 = new ArrayList<>();
        if (posNodesEmMapa.size() > 0) {
            aux2.add(getPosNodesEmMapa().get(0));//aux.add(getNodes().get(0));
        }
        boolean rep;
        for (int a = 1; a < posNodesEmMapa.size(); a++) {
            rep = false;
            for (int x = (a - 1); x >= 0 && (x >= (a - 3)); x--) {
                if (posNodesEmMapa.get(a).equals(posNodesEmMapa.get(x))) {
                    rep = true;
                    x = -5; //sai do laço
                }
            }
            if (!rep) {
                aux2.add(posNodesEmMapa.get(a)); //aux.add(getNodes().get(a));
            }
        }

        conectados = 0;
        posNodesEmMapa = new ArrayList<>();
        
        for (int z = 0; z < aux2.size(); z++) {
            posNodesEmMapa.add(aux2.get(z));
            if (z > 0) {
                if (mape.existeVizinho(posNodesEmMapa.get(z - 1), posNodesEmMapa.get(z), usarGemeos)) {
                    conectados++;
                }else{
                    s = s + "nó "+z+  " para "+(z+1)+"\n";
                    s = s + (mape.getNode_lat()[posNodesEmMapa.get(z - 1)]+"," +mape.getNode_lon()[posNodesEmMapa.get(z -1)])+"\n";
                    s = s +(mape.getNode_lat()[posNodesEmMapa.get(z)]+"," +mape.getNode_lon()[posNodesEmMapa.get(z)])+"\n";
                }
            }
        }
        percConexao = 100*conectados/(posNodesEmMapa.size()-1);
        return s;
    }

    double[] nos;
    boolean encontrou;
    int[] caminhoAux;
    int caminhoAuxTam;

    int maxSaltos;
    double distanciaMax = 99999.0;

    int return_antecipado = 0;
    int return_caminhoMaior = 0;
    int return_nova_solucao = 0;

    public void preparaBusca(int nodes) {
        nos = new double[nodes];
        for (int a = 1; a < nodes; a++) {
            nos[a] = 999900.0;//(short)nodes;
        }

        distanciaMax = 99999.0;
        caminhoAuxTam = 0;
        maxSaltos = 30;
        caminhoAux = new int[maxSaltos + 1];
        encontrou = false; // cuidado. Pode pegar o primeiro caminho que encontrar

    }

    //quer encontrar nó FIND. recebe lista de nós para procurar.
    public void searchWayDistancias(int find, int atual, int ultimaAresta, int[] path, int saltos, double distancia) {

        //  System.out.println("searchWay("+find+", "+atual+", .., "+distancia+")");
        if (distancia >= distanciaMax || encontrou || saltos >= maxSaltos) /*||distancia>nos[atual] )*/ {
            return_antecipado++;
            return;
        }

        int[] path2 = new int[maxSaltos + 1];// = path;
        System.arraycopy(path, 0, path2, 0, saltos); // para que path seja por valor, e não referência/ponteiro

        if (distancia < nos[atual]) // se achou um caminho mais curto para o nó
        {
            nos[atual] = distancia; //nós = distancia do nó 
        } else {
            return_caminhoMaior++;
            return;  //já existe caminho mais curto para esse nó
        }

        path2[saltos] = atual; //acrescenta nó atual ao caminho

        ArrayList<Integer> vizinhosNodes = tempMap.getVizinhosNodesDaAresta(ultimaAresta, atual);
        ArrayList<Integer> vizinhosArestas =  tempMap.getVizinhosArestasDaAresta(ultimaAresta, atual);
        
        //vê se está na vizinhança;
        for (int a = 0; a < vizinhosNodes.size(); a++) {
            if (vizinhosNodes.get(a) == find) { //se há um vizinho que é o destino
                //se estiver, retorna caminho até aqui
                distanciaMax = distancia;
                caminhoAuxTam = (saltos + 1);
                path2[saltos + 1] = find;
                for (int z = 0; z < caminhoAuxTam; z++) {
                    caminhoAux[z] = path2[z];
                }
                // encontrou = true;
                //System.out.println("Encontrou caminho tam "+caminhoAuxTam);
                return_nova_solucao++;
                return;
            }
        }
        //if(vizinhosNodes.size() != vizinhosArestas.size())System.out.println(vizinhosNodes.size()+" "+vizinhosArestas.size()+ " ult "+ultimaAresta+" nod "+atual);  
        //se não estiver, indica próximos nós a buscar
        for (int ab = 0; ab < vizinhosNodes.size(); ab++) {

            searchWayDistancias(find, vizinhosNodes.get(ab), vizinhosArestas.get(ab),
                    path2, (saltos + 1), distancia + tempMap.distanciaMNodes(atual, vizinhosNodes.get(ab)));

        }

    }

    //quer encontrar nó FIND. recebe lista de nós para procurar.
    public void searchWaySaltos(int find, int atual, int[] path, int salto) {

        //  System.out.println("searchWay("+find+", "+atual+", .., "+distancia+")");
        if (salto >= maxSaltos || encontrou) /*||distancia>nos[atual] )*/ {
            return_antecipado++;
            return;
        }

        int[] path2 = new int[maxSaltos + 1];// = path;
        System.arraycopy(path, 0, path2, 0, salto); // para que path seja por valor, e não referência/ponteiro

        if (nos[atual] > salto) // ???????????????????????????????????????????????
        {
            nos[atual] = salto; //nós = distancia do nó 
        } else {
            return_caminhoMaior++;
            return;  //já existe caminho mais curto para esse nó
        }
        path2[salto] = atual; //acrescenta nó atual ao caminho
        ArrayList<Integer> vizinhos = tempMap.getVizinhosIndexDoNode(atual, usarGemeos);

        //vê se está na vizinhança;
        for (int a = 0; a < tempMap.getContVizinhosNode(atual, usarGemeos); a++) {
            if ((vizinhos.get(a)) == find) { //se há um vizinho que é o destino
                //se estiver, retorna caminho até aqui
                maxSaltos = salto;
                caminhoAuxTam = salto + 1;
                path2[salto + 1] = find;
                System.arraycopy(path2, 0, caminhoAux, 0, caminhoAuxTam);
                // encontrou = true;
                //System.out.println("Encontrou caminho tam "+caminhoAuxTam+ "arestas");
                return_nova_solucao++;
                return;
            }
        }

        //se não estiver, indica próximos nós a buscar
        for (int ab = 0; ab < tempMap.getContVizinhosNode(atual, usarGemeos); ab++) {

            searchWaySaltos(find, vizinhos.get(ab), path2, salto + 1);

        }

    }

    public boolean nodeVizinhoEmWay(String nd1, String nd2, int indexWay, ArrayList<String>[] way_nodes, boolean[] oneWay) {

        return way_nodes[indexWay].contains(nd1) && way_nodes[indexWay].contains(nd2);
    }

    public void incluirNode(String cod, int pos) {
        ArrayList<String> aux = new ArrayList<>();
        ArrayList<Integer> aux2 = new ArrayList<>();

        for (int a = 0; a < pos; a++) {
            //aux.add(nodes.get(a));
            aux2.add(posNodesEmMapa.get(a));
        }
        aux.add(cod);
        aux2.add(-1);
        for (int a = pos; a < posNodesEmMapa.size(); a++) {
            //aux.add(nodes.get(a));
            aux2.add(posNodesEmMapa.get(a));
        }

        //nodes = aux;
        posNodesEmMapa = aux2;

    }

    public String getDataInicioString() {

        return hora_inicio.getDay() + "/" + hora_inicio.getMonth() + "/" + hora_inicio.getYear();
        /*Calendar c = Calendar.getInstance();
            c.setTime(hora_inicio);
            return c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR);*/
    }

    String motivoExc = "";

    public String getMotivoExclusao() {
        return motivoExc;
    }

    public boolean deve_ser_mantida(double minMinCorrida, boolean mantemComMissing_data,
            boolean weekdays, boolean weekends, int minimoNodes, boolean testeNodes,
            int minInicio, int minFim, int minInicio2, int minFim2, boolean rigido) {

        if (!weekdays || !weekends) {
            Calendar c = Calendar.getInstance();
            c.setTime(hora_inicio);
            int d = c.get(Calendar.DAY_OF_WEEK);

            if (!weekends && (d == 1 || d == 7 || day_type.toString().equals("B"))) { //B days = feriados
                motivoExc = "Feriado";
                return false;
            }

            if (!weekdays && ((d != 1 && d != 7) || (day_type.toString().equals("A") || day_type.toString().equals("C")))) { //A e C days = dias comuns
                motivoExc = "Weekday";
                return false;
            }

        }

        int min = hora_inicio.getHours() * 60 + hora_inicio.getMinutes();
        //if(!((min<minFim && min>minInicio) || (min<minFim2 && min>minInicio2) )){
        if (min > minFim || min < minInicio) {
            motivoExc = "Horario";
            return false;
        }

        if (testeNodes && rigido) {
            if (posNodesEmMapa.size() < minimoNodes) {
                motivoExc = "Poucos nodes.";
                return false;
            }
        }

        if (getCont_pos() < (minMinCorrida / registrosPorMinuto)) {
            motivoExc = "Corrida curta (s)";
            return false;
        }

        if (missing_data.equals('T') && !mantemComMissing_data && rigido) {
            motivoExc = "Missing data";
            return false;
        }

        if (hora_inicio.getYear() == 2014) {
            if (hora_inicio.getMonth() == 5) {
                if (hora_inicio.getDay() == 4 || hora_inicio.getDay() == 6 || hora_inicio.getDay() == 8 || hora_inicio.getDay() == 9 || hora_inicio.getDay() == 7 || hora_inicio.getDay() == 5 || hora_inicio.getDay() == 11) {
                    motivoExc = "DiaTabu";
                    return false;
                }
            } else if (hora_inicio.getMonth() == 2) {
                if (hora_inicio.getDay() == 20 || hora_inicio.getDay() == 14) {
                    motivoExc = "DiaTabu";
                    return false;
                }
            } else if (hora_inicio.getMonth() == 6 && hora_inicio.getDay() == 5) {
                motivoExc = "DiaTabu";
                return false;
            }
        }

        return !semRegistros;
    }

    

    public double distanciaDaCorridaM() {

        if (getCont_pos() > 0) {
            return distanciaPontos(getLat()[0], getLon()[0], getLat()[getCont_pos() - 1], getLon()[getCont_pos() - 1]) / 0.0000089;
        } else {
            return -1;
        }

    }

    public double duracaoCorridaMin() {

        return ((double) getCont_pos()) / ((double) registrosPorMinuto);

    }

    private boolean node1First(int node1, int node2, int contPos, Double[] node_lat, Double[] node_lon) {

        int antes, depois;

        if (contPos > 0) {
            antes = contPos - 1;
        } else if (cont_pos > (contPos + 1)) {
            antes = contPos;
        } else {
            return true; //só tem um ponto mesmo. Não tem como saber pra onde vai
        }
        depois = antes + 1;
        double distNod1antes = distanciaPontos(node_lat[node1], node_lon[node1], lat[antes], lon[antes]);
        double distNod1depois = distanciaPontos(node_lat[node1], node_lon[node1], lat[depois], lon[depois]);
        //double distNod2antes = distanciaPontos(node_lat[node2], node_lon[node2], lat[antes], lon[antes]);
        // double distNod2depois = distanciaPontos(node_lat[node2], node_lon[node2], lat[depois], lon[depois]);

        return distNod1depois >= distNod1antes/* && distNod2depois > distNod2antes*/; //distancia de nod2, aproxima de nod1 (sentido nod2->nod1)
        /* if (distNod1depois > distNod1antes/* && distNod2depois < distNod2antes/) //distancia de nod1, aproxima de nod2 (sentido nod1->nod2)
        {
        return true;
        }*/
        //carro parado por 15s. Sequência já foi definida. Não há problema
    }

    public boolean mapearTrip(Mapping mapa, boolean porArestas, double metrosRaioNode, double rand, double distValidacao, int profBuscaArestas) {

        ArrayList<Double> lats = new ArrayList<>();
        ArrayList<Double> lons = new ArrayList<>();
        for (int p = 0; p < cont_pos; p++) {
            lats.add(lat[p]);
            lons.add(lon[p]);
        }

        if(porArestas)
            posNodesEmMapa = mapa.calcIndexNOSQuePassaPelasArestas(lats, lons, metrosRaioNode, false, distValidacao, false,profBuscaArestas);
        else
            posNodesEmMapa = mapa.calcIndexNOSQuePassaPelosNos(lats, lons, metrosRaioNode, rand);

        //eliminar nodes repetidos da trip
        ArrayList<Integer> aux2 = new ArrayList<>();
        if (posNodesEmMapa.size() > 0) {
            aux2.add(getPosNodesEmMapa().get(0));//aux.add(getNodes().get(0));
        }
        boolean rep;
        for (int a = 1; a < posNodesEmMapa.size(); a++) {
            rep = false;
            for (int x = (a - 1); x >= 0 && (x >= (a - 3)); x--) {
                if (posNodesEmMapa.get(a).equals(posNodesEmMapa.get(x))) {
                    rep = true;
                    x = -1;
                }
            }
            if (!rep) {
                aux2.add(posNodesEmMapa.get(a)); //aux.add(getNodes().get(a));
            }
        }

        //  System.out.println("Terminou de repassar nodes "+n+":"+horaAtual());
        //nodes = aux;
        posNodesEmMapa = aux2;

        return posNodesEmMapa.size() >= 1;
    }

    /**
     * @param mapa grafo do mapa
     * @deprecated
     * @return if there is at least 3 nodes
     *
     */
    public boolean mapearTripOld(Mapping mapa) {

        //nodes = new ArrayList<>();
        posNodesEmMapa = new ArrayList<>();
        // ways = new ArrayList<>();
        //waysEmMapa = new ArrayList<>();
        //int medPontosPerto = 0;

        String[] node_id = mapa.getNode_id();
        Double[] node_lat = mapa.getNode_lat();
        Double[] node_lon = mapa.getNode_lon();
        int contNodes = mapa.getContNodes();
        ArrayList<Integer> nodesCand;
        ArrayList<Integer> viz;
        double latIt, lonIt;

        //System.out.println("contNodes = "+contNodes+"; cont_pos = "+cont_pos);
        //encontrar nodes pelos quais a trip passa (1m = 0.0000089)
        for (int x = 0; x < cont_pos; x++) {
            nodesCand = new ArrayList<>();
            for (int y = 0; y < contNodes; y++) {
                if (mod(node_lat[y], getLat()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                    if (mod(node_lon[y], getLon()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                        nodesCand.add(y);
                    }
                } else if (mod(node_lat[y], getLat()[x]) < (((double) metrosRaioNode * 5) * 0.0000089)) { // se não está tão longe assim...
                    if (mod(node_lon[y], getLon()[x]) < (((double) metrosRaioNode * 5) * 0.0000089)) {
                        //usando vizinhos, encontra meios termo
                        viz = mapa.getVizinhosIndexDoNode(y, false);
                        for (int v = 0; v < viz.size(); v++) {
                            latIt = (node_lat[y] + node_lat[viz.get(v)]) / 2;
                            lonIt = (node_lon[y] + node_lon[viz.get(v)]) / 2;
                            if (mod(latIt, getLat()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                                if (mod(lonIt, getLon()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                                    if (node1First(y, viz.get(v), x, node_lat, node_lon)) {
                                        nodesCand.add(y);
                                        nodesCand.add(viz.get(v));
                                    } else {
                                        nodesCand.add(viz.get(v));
                                        nodesCand.add(y);
                                    }
                                    v = viz.size();
                                }
                            } //latIt = ((node_lat[y] * 3 + node_lat[viz[v]]) / 4);
                            //lonIt = ((node_lon[y] * 3 + node_lon[viz[v]]) / 4);
                            else if (mod(((node_lat[y] * 3 + node_lat[viz.get(v)]) / 4), getLat()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                                if (mod(((node_lon[y] * 3 + node_lon[viz.get(v)]) / 4), getLon()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                                    if (node1First(y, viz.get(v), x, node_lat, node_lon)) {
                                        nodesCand.add(y);
                                        nodesCand.add(viz.get(v));
                                    } else {
                                        nodesCand.add(viz.get(v));
                                        nodesCand.add(y);
                                    }
                                    v = viz.size();
                                }
                            } //latIt = ((node_lat[y] + node_lat[viz[v]] * 3) / 4);
                            //lonIt = ((node_lon[y] + node_lon[viz[v]] * 3) / 4);
                            else if (mod(((node_lat[y] + node_lat[viz.get(v)] * 3) / 4), getLat()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                                if (mod(((node_lon[y] + node_lon[viz.get(v)] * 3) / 4), getLon()[x]) < (((double) metrosRaioNode) * 0.0000089)) {
                                    if (node1First(y, viz.get(v), x, node_lat, node_lon)) {
                                        nodesCand.add(y);
                                        nodesCand.add(viz.get(v));
                                    } else {
                                        nodesCand.add(viz.get(v));
                                        nodesCand.add(y);
                                    }
                                    v = viz.size();
                                }
                            }
                        }
                    }
                }

            }//acabou laço de analisar MAPnodes

            if (!nodesCand.isEmpty()) {      //se há nodes candidatos para a posição registada X          

                //encontra node que é mais próximo
                //medPontosPerto += nodesCand.size();
                double menorDist = distanciaPontos(getLat()[x], getLon()[x], node_lat[nodesCand.get(0)], node_lon[nodesCand.get(0)]);
                int indexMaisProx = nodesCand.get(0);
                double aux;

                for (int a = 1; a < nodesCand.size(); a++) {
                    aux = distanciaPontos(getLat()[x], getLon()[x], node_lat[nodesCand.get(a)], node_lon[nodesCand.get(a)]);
                    if (aux < menorDist) {
                        indexMaisProx = nodesCand.get(a);
                        menorDist = aux;
                    }
                }

                //adiciona node mais próximo    
                //nodes.add(node_id[indexMaisProx]);
                posNodesEmMapa.add(indexMaisProx);
                // System.out.println("Add node "+indexMaisProx+" ("+node_lat[indexMaisProx]+", "+node_lon[indexMaisProx]+") para "+getLat()[x]+",  "+getLon()[x]);

            }

        }//acabou de repassar pontos

        // System.out.println("Terminou de encontrar nodes "+n+":"+horaAtual());
        //eliminar nodes repetidos da trip
        ArrayList<Integer> aux2 = new ArrayList<>();
        if (posNodesEmMapa.size() > 0) {
            aux2.add(getPosNodesEmMapa().get(0));//aux.add(getNodes().get(0));
        }
        boolean rep;
        for (int a = 1; a < posNodesEmMapa.size(); a++) {
            rep = false;
            for (int x = (a - 1); x >= 0 && (x >= (a - 3)); x--) {
                if (posNodesEmMapa.get(a).equals(posNodesEmMapa.get(x))) {
                    rep = true;
                    x = -1;
                }
            }
            if (!rep) {
                aux2.add(posNodesEmMapa.get(a)); //aux.add(getNodes().get(a));
            }
        }

        //  System.out.println("Terminou de repassar nodes "+n+":"+horaAtual());
        //nodes = aux;
        posNodesEmMapa = aux2;

        if (posNodesEmMapa.size() < 1) {
            return false;
        }

        //eliminar dados de nodes
        node_id = null;
        node_lat = null;
        node_lon = null;
        /*  String[] way_id = mapa.getWay_id();
        ArrayList<String>[] way_nodes = mapa.getWay_nodes();
        boolean[] oneWay = mapa.getOneWay();
        int waysCount = mapa.getWayCount();

        //System.out.println("contNodes = "+nodes.size()+"; cont_ways = "+waysCount);
        //encontrar ways que passam pelos nodes encontrados
        for (int a = 1; a < getNodes().size(); a++) { //de node em node da trip
            for (int w = 0; w < waysCount; w++) { //passa por todos os ways.
                if (oneWay[w]) { //Se Way apenas de ida

                    for (int z = 0; z < way_nodes[w].size(); z++) { //passa pelos nodes do way
                        if (way_nodes[w].get(z).equals(getNodes().get(a - 1))) { //se encontrou um node do way que
                            //é o mesmo da trip

                            if (mapa.nodeComApenasUmWay(getPosNodesEmMapa().get(a)) && way_nodes[w].contains(getNodes().get(a))) { //se o node em questão só é encontrado neste way, com certeza está no way
                                ways.add(way_id[w]);
                                waysEmMapa.add(w);
                            } else { //se não for o caso, ainda pode haver um node a seguir que indica em que way está
                                for (int z2 = z + 1; z2 < way_nodes[w].size(); z2++) { //se o node seguinte é do mesmo way 
                                    if (way_nodes[w].get(z2).equals(getNodes().get(a))) {
                                        ways.add(way_id[w]);
                                        waysEmMapa.add(w);
                                        z2 = way_nodes[w].size();
                                    }
                                }
                            }

                            // z = way_nodes[w].size(); //???
                        }

                    }

                } else //Se Way é nos dois sentidos
                {
                    if (mapa.nodeComApenasUmWay(getPosNodesEmMapa().get(a)) && way_nodes[w].contains(getNodes().get(a))) { //se o node em questão só é encontrado neste way, com certeza está no way
                        ways.add(way_id[w]);
                        waysEmMapa.add(w);
                    } else if (way_nodes[w].contains(getNodes().get(a)) //se o way contém os dois nodes, em qualquer posição
                            && way_nodes[w].contains(getNodes().get(a - 1))) {
                        ways.add(way_id[w]);
                        waysEmMapa.add(w);
                    }
                }
            }

        }

    //    System.out.println("Terminou de encontrar ways "+horaAtual());
        
        //retirar ways repetidos
        aux = new ArrayList<>();
        if (ways.size() > 0) {
            aux.add(ways.get(0));

        }
        for (int a = 1; a < ways.size(); a++) {
            if (!ways.get(a).equals(ways.get(a - 1))) {
                aux.add(ways.get(a));
            }
        }
        ways = aux;*/

        return true;
    }

    public double getMenorLatNodes(Mapping map){
    double lat1 = 100;
    for(int p=0;p<posNodesEmMapa.size();p++)
        if(map.getNode_lat()[posNodesEmMapa.get(p)]<lat1)
            lat1 = map.getNode_lat()[posNodesEmMapa.get(p)];
    return lat1;
    }
    public double getMaiorLatNodes(Mapping map){
    double lat1 = -100;
    for(int p=0;p<posNodesEmMapa.size();p++)
        if(map.getNode_lat()[posNodesEmMapa.get(p)]>lat1)
            lat1 = map.getNode_lat()[posNodesEmMapa.get(p)];
    return lat1;
    }
    
    public double getMenorLonNodes(Mapping map){
    double lon1 = 100;
    for(int p=0;p<posNodesEmMapa.size();p++)
        if(map.getNode_lon()[posNodesEmMapa.get(p)]<lon1)
            lon1 = map.getNode_lon()[posNodesEmMapa.get(p)];
    return lon1;
    }
    public double getMaiorLonNodes(Mapping map){
    double lon1 = -100;
    for(int p=0;p<posNodesEmMapa.size();p++)
        if(map.getNode_lon()[posNodesEmMapa.get(p)]>lon1)
            lon1 = map.getNode_lon()[posNodesEmMapa.get(p)];
    return lon1;
    }
    
    
    private double mod(double x, double y) {
        if (x > y) {
            return (x - y);
        } else {
            return (y - x);
        }
    }

    public int getNodesCount() {
        return posNodesEmMapa.size();
    }

    int nosIniciais;

    public int getContNosInicias() {
        return nosIniciais;
    }

    public taxi_Trip_Instance(ArrayList<Integer> nos, Mapping mape, int raioMetros, boolean pruning, boolean ciclo, boolean restricoesXmlSumo) {

        lat = new Double[1];
        lon = new Double[1];
        call_type = 's';
        day_type = 'b';
        missing_data = 'F';
        hora_inicio = new Timestamp(1 * 1000);
        metrosRaioNode = (byte) raioMetros;
        registrosPorMinuto = 10;
        taxi_id = "s";

        posNodesEmMapa = new ArrayList<>();

        for (int x = 0; x < nos.size(); x++) {
            posNodesEmMapa.add(nos.get(x));//posNodesEmMapa.add(mape.getIndexNode(nos.get(x)));
        }

        if (posNodesEmMapa.size() < 2) {
            posNodesEmMapa = new ArrayList<>();
            return;
        }

        construirCaminhosIncompletos(mape, false, pruning, ciclo,restricoesXmlSumo,25);

        if (posNodesEmMapa.size() < 3) {
            posNodesEmMapa = new ArrayList<>();
        }

        percConexao = (100.0 * (double) conectados) / (double) (posNodesEmMapa.size() - 1);

    }

    public void printRelatorioConectividade() {
        System.out.println(getRelatorioConectividade());
    }

    private String name = "";
    public void setName(String n){
    name = n;
    }
    
    
    public String getRelatorioConectividade() {

        return "OK: "+name+" Comecou com " + nosIniciais + " nós, acabou com " + posNodesEmMapa.size() + " nós! "
                + "" + conectados + "/" + (posNodesEmMapa.size() - 1) + " (" + percConexao + "%) arestas conectadas! ";
                //+ "(RETURNS: antecipado: " + return_antecipado + "; caminhoMaior: " + return_caminhoMaior + "; nova_solucao: " + return_nova_solucao + ")  "+horaAtual();

    }

    double percConexao;

    public double getPercConexao() {
        return percConexao;
    }

    public double getFitnesseConexaoNumNos(){
        return posNodesEmMapa.size()*percConexao*percConexao; //mais nós, melhor. Porc conexão ao quadrado
    }
    
    
    
    public taxi_Trip_Instance(String[] r, int raioMetros, int regPorMin) {

        /* "TRIP_ID","CALL_TYPE","ORIGIN_CALL","ORIGIN_STAND","TAXI_ID","TIMESTAMP","DAY_TYPE","MISSING_DATA","POLYLINE"
            "1372636858620000589","C","","","20000589","1372636858","A","False","[[-8.618643,41.141412],[-......  */
        registrosPorMinuto = (byte) regPorMin;
        metrosRaioNode = (byte) raioMetros;
        //trip_id = r[0];
        call_type = r[1].charAt(1);
        day_type = r[6].charAt(1);
        missing_data = r[7].charAt(1);
        taxi_id = r[4].replace("\"", "");

        long h = Long.valueOf(r[5].replace("\"", ""));
        hora_inicio = new Timestamp(h * 1000);
        //System.out.println(h + " = "+ hora_inicio);

        //lat e lon: Começa em 8.
        int t = (r.length - 8) / 2;
        lat = new Double[t];
        lon = new Double[t];

        if (t == 0) {
            //  System.out.println(trip_id+" sem registros!");
            semRegistros = true;
            return;
        }

        // System.out.println("call_type: "+call_type+"; day_type: "+day_type+"; missing_data: "+missing_data+"; taxi_id: "+taxi_id+"; regs: "+t);
        short cont = 0;
        for (int a = 0; a <= ((r.length - 8) / 2); a = a + 2) {
            if (a == 0) {
                lon[cont] = Double.valueOf(r[a + 8].substring(3));
                lat[cont] = Double.valueOf(r[a + 9].substring(0, r[a + 9].length() - 3));
            } else if (a == ((r.length - 8) / 2)) {
                lon[cont] = Double.valueOf(r[a + 8].substring(1));
                lat[cont] = Double.valueOf(r[a + 9].substring(0, r[a + 9].length() - 3));
            } else {
                lon[cont] = Double.valueOf(r[a + 8].substring(1));
                lat[cont] = Double.valueOf(r[a + 9].substring(0, r[a + 9].length() - 2));
            }

            cont++;
        }

        cont_pos = cont;
        // System.out.println(lat[0]+" "+lon[0]+" "+ hora);
    }

    public double getLat(int x) {
        if (x < getLat().length) {
            return getLat()[x];
        } else {
            return -1;
        }
    }

    public double getLon(int x) {
        if (x < getLat().length) {
            return getLon()[x];
        } else {
            return -1;
        }
    }

    /**
     * @return the quadrantePartidaLat
     */
    public int getQuadrantePartidaLat() {
        return quadrantePartidaLat;
    }

    /**
     * @return the quadrantePartidaLon
     */
    public int getQuadrantePartidaLon() {
        return quadrantePartidaLon;
    }

    /**
     * @return the quadranteChegadaLat
     */
    public int getQuadranteChegadaLat() {
        return quadranteChegadaLat;
    }

    /**
     * @return the quadranteChegadaLon
     */
    public int getQuadranteChegadaLon() {
        return quadranteChegadaLon;
    }

    /**
     * @return the hora_inicio
     */
    public Timestamp getHora_inicio() {
        return hora_inicio;
    }

    /**
     * @return the lat
     */
    public Double[] getLat() {
        return lat;
    }

    /**
     * @return the lon
     */
    public Double[] getLon() {
        return lon;
    }

    /**
     * @return the nodes
     */
    /*public ArrayList<String> getNodes() {
        return nodes;
    }*/
    /**
     * @return the posNodesEmMapa
     */
    public ArrayList<Integer> getPosNodesEmMapa() {
        return posNodesEmMapa;
    }

    public String horaAtual() {

        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));

    }

    /**
     * @return the day_type
     */
    public Character getDay_type() {
        return day_type;
    }

    /**
     * @return the necessidadesInclude
     */
    public int getNecessidadesInclude() {
        return necessidadesInclude;
    }

    /**
     * @return the includesEfetivados
     */
    public int getIncludesEfetivados() {
        return includesEfetivados;
    }

    /**
     * @return the taxi_id
     */
    public String getTaxi_id() {
        return taxi_id;
    }

    /**
     * @return the cont_pos
     */
    public int getCont_pos() {
        return cont_pos;
    }

    public int getPedacDiscretTemporal(int discretTemporal) {

        int min = hora_inicio.getHours() * 60 + hora_inicio.getMinutes();
        int minInterv = (24 * 60) / discretTemporal;
        int t = (int) (min / minInterv);

        return t;
    }

    int indiceGambiarra;

    public int getIndiceGambi() {
        return indiceGambiarra;
    }

    public double calcComprimento(Mapping map){
    double compr=0;
    
        for (int ar = 0; ar < posNodesEmMapa.size() - 1; ar++)
            compr += map.distanciaMNodes(posNodesEmMapa.get(ar), posNodesEmMapa.get(ar+1));
        
    return compr;
    }
    
    private int findArestaNoPontoMetros(Mapping map, double p){
     double compr=0.0;
        for (int ar = 0; ar < posNodesEmMapa.size() - 1; ar++){
            compr = compr + map.distanciaMNodes(posNodesEmMapa.get(ar), posNodesEmMapa.get(ar+1));
            if(compr>=p && map.distanciaMNodes(posNodesEmMapa.get(ar), posNodesEmMapa.get(ar+1))>50)
                return ar;
        }
    
        return -1;
    }
    
    
    
    public ArrayList<Integer> gerarPontosParada(Mapping map, ArrayList<Double> lats,ArrayList<Double> lons){
        ArrayList<Integer> pontos = new ArrayList<>();
      
        double distTrajeto = calcComprimento(map);
        
        double distBuscada=distTrajeto/(lats.size()); //primeiro ponto não é no começo. Por isso, 3 pontos geram 3 divisões. Ultimo ponto no final
        
        for(int i=0;i<lats.size()-1;i++){
            
            pontos.add(findArestaNoPontoMetros(map,distBuscada*(i+1)));
        
        }
        
        
        return pontos;
    }
 
    
    public String getSUMOCodeArestaProxima(Mapping map, double lat, double lon, int ultimaAresta) {
        double menorDist = 40.0;
        double dist;
        String aresta = "";

        double coefAngular;
        double coefLinear;

        double coefAngular_inv;
        double coefLinear_inv;

        double x_intersec;
        double y_intersec;

        for (int ar = ultimaAresta; ar < posNodesEmMapa.size() - 1; ar++) { //passando por todas as arestas
            if (map.getArestaExtensaoMETROS(posNodesEmMapa.get(ar), posNodesEmMapa.get(ar + 1)) > 12) {
                if (map.betweenNodes(lat, lon, posNodesEmMapa.get(ar), posNodesEmMapa.get(ar + 1), (10.0 * 0.0000089))) {
                    //m = (y1 - y2)/(x1 - x2);
                    coefAngular = (map.getNode_lon()[posNodesEmMapa.get(ar)] - map.getNode_lon()[posNodesEmMapa.get(ar + 1)])
                            / (map.getNode_lat()[posNodesEmMapa.get(ar)] - map.getNode_lat()[posNodesEmMapa.get(ar + 1)]);
                    //b = Y + m*X
                    coefLinear = map.getNode_lon()[posNodesEmMapa.get(ar)] + coefAngular * map.getNode_lat()[posNodesEmMapa.get(ar)];
                    //EQ RETA: y =  coefAngular*x + coefLinear   //   y = m*x + b

                    coefAngular_inv = 1 / coefAngular;
                    coefLinear_inv = lon + coefAngular_inv * lat;
                    //EQ RETA = y = coefAngular_inv*x + coefLinear_inv

                    //ponto intersec:   0 = (coefAngular-coefAngular_inv)*x + (coefLinear-coefLinear_inv);
                    //ponto intersec:   -(coefLinear-coefLinear_inv) = (coefAngular-coefAngular_inv)*x
                    //ponto_intersec:   x  =  -(coefLinear-coefLinear_inv)/(coefAngular-coefAngular_inv)
                    x_intersec = ((-coefLinear + coefLinear_inv) / (coefAngular - coefAngular_inv));
                    y_intersec = coefAngular_inv * x_intersec + coefLinear_inv;

                    dist = (distanciaPontos(-x_intersec, y_intersec, lat, lon));
                    //System.out.println("lat "+lat+" lon "+lon+"; Aresta "+ar+"; intersec: "+x_intersec+" "+y_intersec+" dist "+dist+"m ");

                    if (dist < menorDist) {

                        menorDist = dist;
                        aresta = map.getSUMOEdgeCode(posNodesEmMapa.get(ar), posNodesEmMapa.get(ar + 1));
                        indiceGambiarra = ar;

                    }

                }
            }
        }

        return aresta;
    }
double LOCAL_PI = 3.1415926535897932385;
    double ToRadians(double degrees) 
{
  double radians = degrees * LOCAL_PI / 180;
  return radians;
}

double distanciaPontos(double lat1, double lng1, double lat2, double lng2) 
{
  double earthRadius = 3958.75;
  double dLat = ToRadians(lat2-lat1);
  double dLng = ToRadians(lng2-lng1);
  double a = Math.sin(dLat/2) * Math.sin(dLat/2) + 
             Math.cos(ToRadians(lat1)) * Math.cos(ToRadians(lat2)) * 
             Math.sin(dLng/2) * Math.sin(dLng/2);
  double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  double dist = earthRadius * c;
  double meterConversion = 1609.00;
  return dist * meterConversion;
}
    
    
}
