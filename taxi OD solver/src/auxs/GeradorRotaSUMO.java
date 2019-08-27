/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import taxi.od.solver.Mapping;
import taxi.od.solver.TaxiODSolver;
import taxi.od.solver.taxi_Trip_Instance;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.LayoutPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Resolutions;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerPipe;

/**
 *
 * @author lucia
 */
public class GeradorRotaSUMO {

    int raioMetros = 9;
    Mapping mapa;
    String OSMFileLocation = "arquivos SUMO\\big_cwb.osm";//"arquivos SUMO\\pinheirinho.osm";
    String SUMOXmlLocation = "arquivos SUMO\\big_cwb.xml";//"arquivos SUMO\\pinheirinho.xml";
    String dump_Location = "arquivos SUMO\\simulation_dump.xml";

    //String rotasDeInteresse = "617,630,631"; //649
   String rotasDeInteresse = "617, 630, 631, 204, 060, 639, 640, 641, 642, 655".replace(" ", "");
   // String rotasDeInteresse = "040, 060, 204, 502, 507, 508, 550, 602, 603, 617, 630, 631, 632, 633, 635, 636, 637, 638, 639, 640, 641, 642, 643, 644, 646, 649, 650, 655, 656, 659, 680, 681, 684, 688, 690, X19".replace(" ", "");
    
    ArrayList<String> linhas; //rotas de interesse
    ArrayList<String> cores;
    String[] linhasDeparts;
    int [] veic;

    ArrayList<String> cadLinha = new ArrayList<>();
    ArrayList<String> codLinha;
    ArrayList<String> codVeh;
    ArrayList<Integer> segundos;
    ArrayList<Double> latV;
    ArrayList<Double> lonV;
    ArrayList<Double> veiculosHora;
    ArrayList<String>[] codigosParadas;
    ArrayList<Double>[] latParadas;
    ArrayList<Double>[] lonParadas;
    ArrayList<Integer>[] ordem;
    ArrayList<Integer>[] indexArestaParadas;

    ArrayList<String> horLinha = new ArrayList<>();
    ArrayList<Integer> horSegundos = new ArrayList<>();

    ArrayList<taxi_Trip_Instance> onibus;

    int comecoSeg;
    int fimSeg;

    public GeradorRotaSUMO(int dia, int mes, int ano, double horaComeco, double horaFim) {

        System.out.println("\nPROC: Gerador de código SUMO. Dados de " + dia + "/" + mes + "/" + ano + ", " + ((int) horaComeco) + "-" + ((int) horaFim) + "h. - Inicio em " + horaAtual());

        String diaS = dia + "";
        String mesS = mes + "";
        comecoSeg = (int) (horaComeco * 3600);
        fimSeg = (int) (horaFim * 3600);
        if (dia < 10) {
            diaS = "0" + dia;
        }
        if (mes < 10) {
            mesS = "0" + mes;
        }
        String[] sAux;
        linhas = new ArrayList<>();
        cores = new ArrayList<>();
        sAux = rotasDeInteresse.split(",");
        for (int a = 0; a < sAux.length; a++) {
            linhas.add(sAux[a]);
            cores.add(getCor(a));
        }
        linhasDeparts = new String[linhas.size()];
        veic = new int[linhas.size()];

        //carregar objeto mapping
        loadOSMData();
        //mapa.relatArestaSUMO("166479502#1");  // 166479502#1' and edge '125207637#2
        //mapa.relatArestaSUMO("125207637#2"); //166479502#1' and edge '125207637#2

        //carregar dados de movimentação de ônibus (Ex: 2017_07_26_veiculos.json) 
        lerDadosMovimentacaoBus(diaS, mesS, ano+"");
        lerDadosShapeLinhas(diaS, mesS, ano + "");
        lerHorariosOnibus(diaS, mesS, ano + "");

        //repassar trace para objetos taxi_Trip_Instance
        passarTraceParaObjetos();
        descobrirParadasDosOnibus(diaS, mesS, ano + "");

        //gerar arquivo XML SUMO
        gerarXmlSUMO();

        //processar dump anterior
        processarDumpFile();
    }

    int stopLaneNaoIdent;
    int naoAchouEdge;
    int achouEdge;
    int stopLaneIdent;

    private void gerarXmlSUMO() {

        ArrayList<String> flows = new ArrayList<>();
        ArrayList<Integer> tempoFlows = new ArrayList<>();

        String declaracaoRotas = "";
        String declaracaoFlows = "";

        stopLaneIdent = 0;
        stopLaneNaoIdent = 0;
        naoAchouEdge = 0;
        achouEdge = 0;

        String details = "";
        int somaVec = 0;
        int somaDepart = 0;
        for (int l = 0; l < linhas.size(); l++) {
            details = details + linhas.get(l) + " ";
            somaVec += veic[l];
            somaDepart = departs[l].size();
        }

        if (veiculosHora.isEmpty()) {
            //   System.out.println("ERROR: Quantidade de veiculos por linha nao definida. Gerando aleatoriamente (geraXmlSUMO)");
            while (veiculosHora.size() < codLinha.size()) {
                veiculosHora.add(5 + Math.random() * 20);
            }
        }

        for (int casx = 0; casx < onibus.size(); casx++) {
            declaracaoRotas = declaracaoRotas + declaracaoRota(casx) + "\n";
            flows.add(declaracaoFluxo(casx));
            tempoFlows.add(segundos.get(casx));
        }

        //passando de 1 em seg, chama flows adequados
        for (int t = comecoSeg; t < fimSeg; t++) {
            for (int c = 0; c < tempoFlows.size(); c++) {
                if (tempoFlows.get(c) == t) {// se for do tempo certo{
                    declaracaoFlows = declaracaoFlows + flows.get(c) + "\n";
                    flows.remove(c);
                    tempoFlows.remove(c);
                    c--;
                }
            }
        }
        String inf = "<!-- Lat ["+this.getMenorLatTraj()+", "+this.getMaiorLatTraj()+"] Lon ["+this.getMenorLonTraj()+", "+this.getMaiorLonTraj()+"] "
                + "\nLat "+ mapa.distanciaPontosMETROS(getMenorLatTraj(), getMenorLonTraj(), getMaiorLatTraj(), getMenorLonTraj())+"m, Lon "+mapa.distanciaPontosMETROS(getMenorLatTraj(), getMenorLonTraj(), getMenorLatTraj(), getMaiorLonTraj())+"m"
                + "\n"+somaVec+" nos/veiculos diferentes; "+somaDepart+" partidas;"
                + "\nLinhas " + rotasDeInteresse + ". " + stopLaneIdent + "/" + (stopLaneNaoIdent + stopLaneIdent) + " "
                + "(" + (100 * stopLaneIdent) / (stopLaneNaoIdent + stopLaneIdent) + "%) paradas identificadas. " + achouEdge + "/" + (achouEdge + naoAchouEdge) + " "
                + "(" + (100.0 * ((double) achouEdge) / (double) (achouEdge + naoAchouEdge)) + "%) edges identificadas!\n";
        System.out.println(inf);

        for (int l = 0; l < linhas.size(); l++) {
            inf = inf + onibus.get(l).getRelatorioConectividade() +"; " +linhasDeparts[l]+  "\n";
        }

        inf = inf.substring(0, inf.length() - 2) + "-->\n";

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + inf + "\n"
                + "<routes>"
                + "\n<vType sigma=\"0.5\" maxSpeed=\"30.0\" minGap=\"2.25\" length=\"10.0\" id=\"bus\" vClass=\"bus\" decel=\"4.0\" accel=\"0.9\"/>"
                + "\n<vType sigma=\"0.5\" maxSpeed=\"45.0\" minGap=\"1.25\" length=\"4.0\" id=\"kadett\" decel=\"7.0\" accel=\"1.8\"/>"
                + "\n<vType sigma=\"0.5\" maxSpeed=\"16.0\" minGap=\"2.5\" length=\"35.0\" id=\"biarticulado\" decel=\"5.0\" accel=\"1.0\"/>"
                + "\n" + declaracaoRotas + "\n" + declaracaoVeiculos() /* declaracaoFlows*/ + "\n</routes>";

        salvar(xml, "routes_" + linhas.size() + "_linhas");

        // return rotas;
    }

    private void addArestaParada(String aresta, int indiceNoTrace, int index, double lat, double lon) {

        //if (index != -1) {
        codigosParadas[index].add(aresta);
        indexArestaParadas[index].add(index); //?????
        latParadas[index].add(lat);
        lonParadas[index].add(lon);
        ordem[index].add(indiceNoTrace);

        //  }
    }

    private void descobrirParadasDosOnibus(String dia, String mes, String ano) {
        //{"NOME":"Av. Sete de Setembro, 6001 - Batel","NUM":"180724","LAT":"-25.4478500705","LON":"-49.293519587464","SEQ":"8","GRUPO":"","SENTIDO":"V. Sandra","TIPO":"Chapeu chines","COD":"860"}
        //0  1   2           3                        4  5  6   7    8  9  10     11       12 13 14       15        16 17 18 19 20 21 22 24  25    26     27   28  29  30    31        32 33 34 35  36=0                                           
        int cont = 0;
        codigosParadas = new ArrayList[linhas.size()];
        indexArestaParadas = new ArrayList[linhas.size()];
        latParadas = new ArrayList[linhas.size()];
        lonParadas = new ArrayList[linhas.size()];
        ordem = new ArrayList[linhas.size()];

        for (int i = 0; i < linhas.size(); i++) {
            codigosParadas[i] = new ArrayList<>();
            indexArestaParadas[i] = new ArrayList<>();
            latParadas[i] = new ArrayList<>();
            lonParadas[i] = new ArrayList<>();
            ordem[i] = new ArrayList<>();
        }

        String file = "arquivos SUMO\\Urbs\\" + ano + "_" + mes + "_" + dia + "_pontosLinha.json";

        try {
            Scanner scanner;
            scanner = new Scanner(new File(file));
            scanner.useDelimiter("\\Z");
            file = scanner.next();
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: Não encontrou " + file + "!");
            return;
        }

        String[] all = file.split("NOME");
        String[] sep;// = file.split("\"");
        String ars;
        int inx = 0;
        int gant;
        int errow = 0;

        for (int a = 1; a < all.length; a++) {

            sep = all[a].split("\"");
            if (linhas.contains(sep[34])) {
                //ars = mapa.getIndexArestaProxima(Double.valueOf(sep[10]), Double.valueOf(sep[14]), 20.0, -1, -1.0, -1.0);
                gant = inx;
                inx = linhas.indexOf(sep[34]);
                ars = onibus.get(inx).getSUMOCodeArestaProxima(mapa, Double.valueOf(sep[10]), Double.valueOf(sep[14]), gant);

                addArestaParada(ars, onibus.get(inx).getIndiceGambi(), inx, Double.valueOf(sep[10]), Double.valueOf(sep[14]));
                cont++;
                if (ars.length() < 3) {
                    errow++;
                }

            }

        }

        System.out.println("OK: Acabou de cadastrar " + cont + " paradas de onibus; " + errow + " não acharam arestas." + horaAtual());

    }

    private String getStringLatLonParadas(int linhaNum) {
        String s = "";
        for (int x = 0; x < latParadas[linhaNum].size(); x++) {
            s = s + latParadas[linhaNum].get(x) + ", " + lonParadas[linhaNum].get(x) + "\n";
        }

        return s.substring(0, s.length() - 2);
    }

    private String declaracaoRota(int n) {
        //<route id="rota0.horizontal_1.Cnv0" edges="263504521#1 231107712#1 231107712#2 231107712#3 263504524 263504522#0 263504522#1 263504522#2 263504522#3 263504522#4"/>
        //<stop lane="263504522#1" endPos="-30" duration="20"/> </route>
        ArrayList<Integer> pontos = onibus.get(n).gerarPontosParada(mapa, latParadas[n], lonParadas[n]);;
        ArrayList<Integer> nos = onibus.get(n).getPosNodesEmMapa();
        System.out.println(onibus.get(n).eliminarNodesRepetidos(mapa)); //apenas para mostrar  falhas.
        String r = "";
        String aux;
        ArrayList<String> arestax = new ArrayList<>();

        ArrayList<String> terminal = new ArrayList<>();
        terminal.add("-161917487");
        terminal.add("161917487");
        terminal.add("-161917483");
        terminal.add("161917483");
        terminal.add("-161917485");
        terminal.add("161917485");
        terminal.add("-161917486");
        terminal.add("161917486");
        terminal.add("-24547674#0");
        terminal.add("24547674#0");
        int pontoTerminal = -1;
        linhasDeparts[n] = "\nIntervalo "+intervalosMax[n]+" s; "+departs[n].size()+" departs; "+veic[n]+" veiculo(s); ";
        
        for (int no = 0; no < nos.size() - 1; no++) {
            
            aux = mapa.getSUMOEdgeCode(nos.get(no), nos.get(no + 1)).replace(" ", "");

            if (aux.equals("FALHA") || aux.length()<2) {
                naoAchouEdge++;
            } else {

                if (arestax.isEmpty()) {
                    achouEdge++;
                    arestax.add(aux);
                    //localEd++;
                    if (pontoTerminal == -1) {
                        if (terminal.contains(aux)) {
                            pontoTerminal = no;
                        }
                    }
                } else if (!arestax.get(arestax.size() - 1).equals(aux)) {
                    achouEdge++;
                    arestax.add(aux);
                    //localEd++;
                    if (pontoTerminal == -1) {
                        if (terminal.contains(aux)) {
                            pontoTerminal = no;
                        }
                    }
                }
            }
        }

        //System.out.println("Linha " + linhas.get(n) + ": terminal em aresta " + pontoTerminal);
        String last = "";
        veic[n] = departs[n].size();
        for (int d = 0; d < departs[n].size(); d++) {
            
            
            int nextDepart = departs[n].get(d) + intervalosMax[n];
            for (int d2 = d + 1; d2 < departs[n].size(); d2++) {
                if ((departs[n].get(d2) - departs[n].get(d)) >= intervalosMax[n]*1.1) {
                    nextDepart = departs[n].get(d2)-1;
                    linhasDeparts[n] = linhasDeparts[n] + d+"("+departs[n].get(d)+ ")="+d2+"("+nextDepart+"); ";
                    veic[n]--;
                    d2 = departs[n].size(); //sair do laço
                }
            }

            String edges = "";
            String stops = "";

            for (int e = pontoTerminal + 1; e < arestax.size(); e++) {

                if (!last.equals(arestax.get(e))) {
                    edges = edges + arestax.get(e) + " ";
                    last = arestax.get(e);
                } else {
                    last = arestax.get(e);
                }

                for (int p = 0; p < pontos.size(); p++) {
                    if (pontos.get(p) != -1) {
                        if (p != (pontos.size() - 1)) {
                            if (arestax.get(pontos.get(p)).equals(arestax.get(e))) {
                                stops = stops + "\n<stop lane=\"" + arestax.get(pontos.get(p)) + "_0\" endPos=\"-5\" duration=\"" + (9 + (int) (Math.random() * 7)) + "\"/>";
                            }
                        }
                    }
                }

            }
            if (pontoTerminal != -1) {
                for (int e = 0; e <= pontoTerminal; e++) {
                    
                    if (!last.equals(arestax.get(e))) {
                        edges = edges + arestax.get(e) + " ";
                        last = arestax.get(e);
                    } else {
                        last = arestax.get(e);
                    }

                    for (int p = 0; p < pontos.size(); p++) {
                        if (pontos.get(p) != -1) {
                            if (p != (pontos.size() - 1)) {

                                if (arestax.get(pontos.get(p)).equals(arestax.get(e))) {

                                    if (e == pontoTerminal) {
                                        stops = stops + "\n<stop lane=\"" + arestax.get(pontos.get(p)) + "_0\" endPos=\"-5\" until=\"" + nextDepart + "\"/>\n";
                                    } else {
                                        stops = stops + "\n<stop lane=\"" + arestax.get(pontos.get(p)) + "_0\" endPos=\"-5\" duration=\"" + (9 + (int) (Math.random() * 7)) + "\"/>";
                                    }
                                }
                            }
                        }
                    }

                }
            }

            edges = edges.substring(0, edges.length() - 1) + "\">";

            r = r + "<route id=\"r" + linhas.get(n) + "x" + d + "\" edges=\"" + edges + stops + "\n</route>\n";

        }

        stopLaneNaoIdent += (latParadas[n].size() - pontos.size());
        stopLaneIdent += pontos.size();

        emTxt(onibus.get(n).demonstrarCaminhoEmMapa(mapa), "arquivos SUMO\\" + linhas.get(n) + " trace");
        emTxt(onibus.get(n).demonstrarNosEmMapa(mapa), "arquivos SUMO\\" + linhas.get(n) + " nos identificados");
        emTxt(getStringLatLonParadas(n), "arquivos SUMO\\" + linhas.get(n) + " pontos de parada");

        /*System.out.println("ROTA "+linhas.get(n)+": Começou com "+onibus.get(n).getContNosInicias()+" nós, "
                + "completou para "+onibus.get(n).getNodesCount()+" nós; "+
                localEd+"/"+(onibus.get(n).getNodesCount()-1)+" ("+(100*localEd/(onibus.get(n).getNodesCount()-1))+"%) arestas conectadas; "
                + localP + "/"+codigosParadas[n].size()+" ("+100*localP/codigosParadas[n].size()+"%) paradas identificadas!"); */
        return r;
    }

    private String declaracaoFluxo(int n) {
        //<flow id="0" color="1,1,0" begin="0" end="50000" vehsPerHour="30" type="kadett" departSpeed="16" departLane="best" departPost="random_free" route ="rota0.horizontal_1.Cnv0"/> 
        return "<flow id=\"" + n + "\" color =\"" + cores.get(n) + "\" begin=\"" + comecoSeg + "\" end=\"" + fimSeg + "\" vehsPerHour=\"" + veiculosHora.get(n) + "\""
                + " type=\"bus\" departSpeed=\"1\" departLane=\"best\" departPost=\"random_free\" route=\"r" + linhas.get(n) + "\"/>";

    }

    private String declaracaoVeiculos() {
        //<vehicle id="0" type="type1" route="route0" depart="0" color="1,0,0"/>
        String s = "";
        /*for (int x = comecoSeg; x < fimSeg; x++) {
            for (int a = 0; a < horLinha.size(); a++) {

                if (horSegundos.get(a) == x) {
                    s = s + "<vehicle id=\"" + horLinha.get(a) + "." + a + "\" type=\"bus\" route=\"" + horLinha.get(a) + "\" depart=\"" + x + "\" color=\"" + horCor.get(a) + "\"/>\n";
                }
            }
        }*/

        for (int x = comecoSeg; x < fimSeg; x++) {
            for (int a = 0; a < linhas.size(); a++) {
                for (int d = 0; d < departs[a].size(); d++) {

                    if (departs[a].get(d) == x) {
                        s = s + "<vehicle id=\"" + linhas.get(a) + "." + d + "\" type=\"bus\" route=\"r" + linhas.get(a) + "x" + d + "\" depart=\"" + x + "\" color=\"" + getCor(a) + "\"/>\n";
                    }

                }
            }
        }

        return s;
    }

    private void passarTraceParaObjetos() {

        System.out.println("PROC: Passando caminho para objetos... " + horaAtual());

        ArrayList<Integer> tabuFrom = new ArrayList<>();
        ArrayList<Integer> tabuTo = new ArrayList<>();
        tabuFrom.add(10664);
        tabuTo.add(2598);

        boolean validacao = true;
        boolean pruning = true;

        if (validacao && pruning) {
            System.out.println("ALERT: Validação (Mapping.calcIndexNosQuePassaPelasArestas) e pruning (Taxi_Trip_Instance.construirCaminhosIncompletos) ativos!");
        } else if (validacao) {
            System.out.println("ALERT: Validacao ativa! (Mapping.calcIndexNosQuePassaPelasArestas)");
        } else if (pruning) {
            System.out.println("ALERT: Pruning ativo! (Taxi_Trip_Instance.construirCaminhosIncompletos)");
        }

        int contN = 0;
        onibus = new ArrayList<>();
        String[] r;
        ArrayList<String> ar = new ArrayList<>();
        ArrayList<Integer> nos;
        ArrayList<Double> gamb;
        ArrayList<Double> gamb2;
        taxi_Trip_Instance aux;

        ArrayList<Double> latsL;
        ArrayList<Double> lonsL;
        double linhaFora = 0;
        int contBug = 0;
        ArrayList<Double> la2 = new ArrayList<>();
        ArrayList<Double> lo2 = new ArrayList<>();
        double maiorLat = -50;
        int indexMaiorLat = -1;

        //System.out.println("ALERT: Dobrando pontos recebidos.");
        for (int i = 0; i < linhas.size(); i++) {
            contBug = 0;
            nos = new ArrayList<>();
            latsL = new ArrayList<>();
            lonsL = new ArrayList<>();

            for (int x = 0; x < codLinha.size(); x++) { //passando pelos pontos de passagem registrados 
                if (codLinha.get(x).equals(linhas.get(i))) {

                    /*if(latsL.size()>0){
                        latsL.add((latsL.get(latsL.size()-1)+latV.get(x))/2);
                        lonsL.add((lonsL.get(lonsL.size()-1)+lonV.get(x))/2);
                    }*/
                    if (i == 3 && contBug < 15) {
                        contBug++;
                        la2.add(latV.get(x));
                        lo2.add(lonV.get(x));
                        //System.out.println(latV.get(x)+", "+ lonV.get(x));
                    } else {

                        if (i == 3 && latV.get(x) > maiorLat) {
                            maiorLat = latV.get(x);
                            indexMaiorLat = x;
                        }

                        // index = mapa.identificarNodeMaisProximoIndex(latV.get(x), lonV.get(x), raioMetros);
                        latsL.add(latV.get(x));
                        lonsL.add(lonV.get(x));
                    }

                    /*  if (index != -1) {
                        nos.add(index);
                    }else{
                        naoAchouNo++;
                       // System.out.println("OFF: ["+latV.get(x)+"; "+lonV.get(x)+"] "
                        //        + "em mapa de lat["+mapa.getMenorLat()+":"+mapa.getMaiorLat()+"]; lon["+mapa.getMenorLon()+":"+mapa.getMaiorLon()+"]");
                    }*/
//
                } else {
                    linhaFora = linhaFora + 1.0 / linhas.size();
                }
            }

            if (i == linhas.indexOf("649")) {

                gamb = new ArrayList<>();
                gamb2 = new ArrayList<>();

                for (int a = 6; a <= indexMaiorLat; a++) {
                    gamb.add(latsL.get(a));
                    gamb2.add(lonsL.get(a));
                }

                System.out.println("ALERT! Rota 649(i==3) tem problemas. IndexMaiorLat = " + indexMaiorLat + " Jogando primeiros " + la2.size() + " traces para lugar correto.");
                /*for (int t = 0; t < la2.size(); t++) {
                    gamb.add(la2.get(t));
                    gamb2.add(lo2.get(t));
                } */

                for (int a = indexMaiorLat + 1; a < latsL.size(); a++) {
                    gamb.add(latsL.get(a));
                    gamb2.add(lonsL.get(a));
                }

                latsL = gamb;
                lonsL = gamb2;

                //  for(int h=0;h<latsL.size()-1;h++)
                //    System.out.println(h+": "+mapa.distanciaPontosMETROS(latsL.get(h), lonsL.get(h), latsL.get(h+1), lonsL.get(h+1))+"m "+latsL.get(h)+","+ lonsL.get(h)+","+ latsL.get(h+1)+","+ lonsL.get(h+1));
            } else {

                gamb = new ArrayList<>();
                gamb2 = new ArrayList<>();
                for (int a = 0; a < latsL.size(); a++) {
                    gamb.add(latsL.get(a));
                    gamb2.add(lonsL.get(a));
                    if (a < latsL.size() - 1) {
                        gamb.add((latsL.get(a) * 2 + latsL.get(a + 1)) / 3);
                        gamb2.add((lonsL.get(a) * 2 + lonsL.get(a + 1)) / 3);

                        gamb.add((latsL.get(a) + latsL.get(a + 1)) / 2);
                        gamb2.add((lonsL.get(a) + lonsL.get(a + 1)) / 2);

                        gamb.add((latsL.get(a) + latsL.get(a + 1) * 2) / 3);
                        gamb2.add((lonsL.get(a) + lonsL.get(a + 1) * 2) / 3);
                    }
                }
                latsL = gamb;
                lonsL = gamb2;
            }

            //encontrar ponto que está no terminal pinheirinho (entre LAT -25.51170 e  -25.51358  LON -49.29580 e -49.29363)
            /*gamb = new ArrayList<>();
            gamb2 = new ArrayList<>();
            int pontoPinheirim=-1;
            for(int rx=0;rx<latsL.size();rx++)
                if(mapa.between(latsL.get(rx), -25.51200, -25.51300, 0.0))
                    if(mapa.between(lonsL.get(rx), -49.29580, -49.29363, 0.0)){
                        pontoPinheirim = rx;
                        rx=latsL.size();
                    }
            
            if(pontoPinheirim==-1){
                pontoPinheirim = 0;
                System.out.println("ALERT: Linha "+linhas.get(i)+ " não achou ponto no terminal pinheirinho.");
            }
            
            for(int rx=pontoPinheirim;rx<latsL.size();rx++){
                gamb.add(latsL.get(rx));
                gamb2.add(lonsL.get(rx));
            }
            for(int rx=0;rx<pontoPinheirim;rx++){
                gamb.add(latsL.get(rx));
                gamb2.add(lonsL.get(rx));
            }
            latsL = gamb;
            lonsL = gamb2;*/
            taxi_Trip_Instance best = null;
            double bestPerc = 0.0;
            double bestD = 0.0;
            int crs = -1;
            best = null;
            
            for (double d = 2.5; d < 8.0; d = d + 0.3) {
                for (int c = 0; c <= 4; c++) { //para "randomização" com variavel ahead

                    if(c == 0){
                        nos = mapa.calcIndexNOSQuePassaPelasNosProximosDasArestas(latsL, lonsL, d, d/6);
                    }
                    else if (c < 3) {
                        nos = mapa.calcIndexNOSQuePassaPelasArestas(latsL, lonsL, d, validacao, 30.0, c > 0, 3);
                    } else {
                        nos = mapa.calcIndexNOSQuePassaPelosNos(latsL, lonsL, d, d / 6);
                    }



 /*nos = new ArrayList<>();
                for(int a=0;a<nos2.size()-1;a++)
                    if(!tabuFrom.contains(nos2.get(a)))
                        nos.add(nos2.get(a)); //não tá em tabuFrom. Ok
                    else if(tabuTo.contains(nos2.get(a+1))){ //X está em tabuFrom. Se (X+1) estiver em tabuTO
                        a++; //Não add o atual, e ainda pula o próximo.
                        System.out.println("ALERT: Retirou aresta tabu!");
                    }else{
                        nos.add(nos2.get(a)); //X em tabuFrom, mas X+1 não está em tabuTo
                    }
                nos.add(nos2.get(nos2.size()-1)); */
                    //reuniu todos os pontos da linha
                    if (nos.size() > numMinNos(i)) {

                        aux = new taxi_Trip_Instance(nos, mapa, raioMetros, pruning,true,true);
                        aux.addPontosLatLon(latsL, lonsL);

                        if ((aux.getPercConexao() > bestPerc)
                                && ((i == 0 && aux.getPosNodesEmMapa().size() > 112) || (i == 1 && aux.getPosNodesEmMapa().size() > 145)
                                || (i == 2 && aux.getPosNodesEmMapa().size() > 60) || (i == 3 && aux.getPosNodesEmMapa().size() > 150)
                                || (i >3 && aux.getPosNodesEmMapa().size() >80) )) {
                            //onibus.add(aux);
                            best = aux;
                            best.setName(linhas.get(i));
                            bestD = d;
                            crs = c;
                            bestPerc = aux.getPercConexao();

                            if (bestPerc == 100.0) {
                                d = 100;
                                c = 3;
                            }

                        }
                    }
                }
            }

            
            System.out.print("ROTA " + linhas.get(i) + " (d=" + bestD + ", c=" + crs + "): ");
            onibus.add(best);
            if(bestD!=0){
                contN += onibus.get(onibus.size() - 1).getNodesCount();
                best.printRelatorioConectividade();
            }
        }

        System.out.println("OK: Acabou de criar " + onibus.size() + "/" + linhas.size() + " objetos de trajetos de onibus! "
                + "(Media " + (contN / onibus.size()) + " nos por objeto) SemLinha: " + linhaFora /*+", SemNoCorrsp: "+naoAchouNo+" "*/ + horaAtual());

    }

    private int numMinNos(int indexLi) {

        if (indexLi == 0) {
            return 50;
        }
        if (indexLi == 1) {
            return 50;
        }
        if (indexLi == 2) {
            return 40;
        }
        //if == 3
        return 50;

    }

    private void lerDadosShapeLinhas(String dia, String mes, String ano) {
        //[{"SHP":"1773","LAT":"-25.3772246","LON":"-49.224577899999986","COD":"209"},
        //0   1  2   3  4  5  6      7      8  9  10      11            12 13 14  15
        String file = "arquivos SUMO\\Urbs\\" + ano + "_" + mes + "_" + dia + "_shapeLinha.json";
        codLinha = new ArrayList<>();
        ArrayList<String> shp = new ArrayList<>();
        for(int l=0;l<linhas.size();l++)
            shp.add("none");
        latV = new ArrayList<>();
        lonV = new ArrayList<>();
        veiculosHora = new ArrayList<>();
        segundos = new ArrayList<>();
        int pontosForaMapa = 0;

        double latX, lonX;

        System.out.println("PROC: Iniciando leitura de " + file + "! " + horaAtual());
        try {
            Scanner scanner;
            scanner = new Scanner(new File(file));
            scanner.useDelimiter("\\Z");
            file = scanner.next();

            String[] all = file.split("},");
            String[] sep;// = file.split("\"");

            for (int a = 0; a < all.length; a++) {

                sep = all[a].split("\"");
                //  System.out.println(sep[15]);
                if (linhas.contains(sep[15])) { //se linha é de interesse

                    if (!cadLinha.contains(sep[15])) { //se é nova, cadLinha e cad o shp
                        cadLinha.add(sep[15]);
                    } 
                    
                    if(shp.get(cadLinha.indexOf(sep[15])).equals("none"))
                        shp.set(cadLinha.indexOf(sep[15]), sep[1]);
                    
                    
                    if(shp.get(cadLinha.indexOf(sep[15])).equals(sep[1])){ //se for do mesmo shp

                    latX = Double.valueOf(sep[7]);
                    lonX = Double.valueOf(sep[11]);

                    if (mapa.ispontoDentroDoMapa(latX, lonX)) {
                        codLinha.add(sep[15]);
                        latV.add(latX);
                        lonV.add(lonX);
                        segundos.add(this.comecoSeg);

                    } else {
                        pontosForaMapa++;
                        System.out.println(latX + ", " + lonX);
                    }
                    
                    }

                } //fim SE linha é de interesse

            }

            System.out.println("OK: " + codLinha.size() + " Localizações distribuidas em " + linhas.size() + " linhas diferentes! (" + pontosForaMapa + " pontos fora do mapa) " + horaAtual());

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    
    double menorLatTraj = 100;
    double menorLonTraj = 100;
    double maiorLonTraj = -100;
    double maiorLatTraj = -100;
    
    
    public double getMenorLonTraj(){
    if(menorLonTraj==100)
        calcMMlatlons();
    return menorLonTraj;
    }
    
    public double getMaiorLonTraj(){
    if(maiorLonTraj==-100)
        calcMMlatlons();
    return maiorLonTraj;
    }
    
    public double getMenorLatTraj(){
    if(menorLatTraj==100)
        calcMMlatlons();
    return menorLatTraj;
    }
    
    public double getMaiorLatTraj(){
    if(maiorLatTraj==-100)
        calcMMlatlons();
    return maiorLatTraj;
    }
    
    public void calcMMlatlons(){
    double v;
        
    for(int o=0;o<onibus.size();o++){
        v = onibus.get(o).getMenorLatNodes(mapa);
        if(v<menorLatTraj)
            menorLatTraj = v;
        
        v = onibus.get(o).getMenorLonNodes(mapa);
        if(v<menorLonTraj)
            menorLonTraj = v;
        
        v = onibus.get(o).getMaiorLonNodes(mapa);
        if(v>maiorLonTraj)
            maiorLonTraj = v;
        
        v = onibus.get(o).getMaiorLatNodes(mapa);
        if(v>maiorLatTraj)
            maiorLatTraj = v;
                
        }
        
    }
    
    
    private void processarDumpFile() {
        //[{"SHP":"1773","LAT":"-25.3772246","LON":"-49.224577899999986","COD":"209"},
        //0   1  2   3  4  5  6      7      8  9  10      11            12 13 14  15

        ArrayList<Double> latVeh = new ArrayList<>();
        ArrayList<Double> lonVeh = new ArrayList<>();

        ArrayList<Double> velVeh = new ArrayList<>();
        ArrayList<Double> tempoReg = new ArrayList<>();
        //ArrayList<String> edgeSUMO = new ArrayList<>();
        ArrayList<String> linhaVeh = new ArrayList<>();
        ArrayList<String> codVeh = new ArrayList<>();
        
        ArrayList<String> encontrosAnt = new ArrayList<>();
        ArrayList<String> encontrosAtual = new ArrayList<>();
        String encontros="";
        int contEncontros = 0;

        /*<edge id="111476405#7">
            <lane id="111476405#7_0">
                <vehicle id="150" pos="227.69" speed="21.20"/>
            </lane>
            <lane id="111476405#7_1">
                <vehicle id="160" pos="212.59" speed="21.02"/>
                <vehicle id="7" pos="250.20" speed="21.92"/>
            </lane>
        </edge>
        <edge id="166479502#5">*/
        String file = this.dump_Location;
        double tempo = -1;
        String edge = "-1";

        System.out.println("PROC: Iniciando leitura de " + file + "! " + horaAtual());
        try {
            Scanner scanner;
            scanner = new Scanner(new File(file));
            scanner.useDelimiter("\\Z");
            file = scanner.next();
            int indexAresta = -1;
            String[] all = file.split("\n");

            for (int a = 0; a < all.length; a++) {

                //identificar hora
                if (all[a].length() > 8) {
                    if (all[a].replace(" ", "").substring(0, 7).equals("<timesteptime=\"2800.00\"/>".substring(0, 7))) {
                        tempo = Double.valueOf(all[a].split("\"")[1]);
                    }
                }

                //identificar edge
                if (all[a].length() > 4) {
                    if (all[a].replace(" ", "").substring(0, 4).equals("<edgeid\"".substring(0, 4))) {
                        edge = all[a].split("\"")[1];
                        indexAresta = mapa.getArestaIndexComCodSUMO(edge);
                    }
                }

                //identificar veiculo e posição na edge
                if (all[a].length() > 4) {
                    if (all[a].replace(" ", "").substring(0, 4).equals("<vehicle\"".substring(0, 4))) {

                        if (indexAresta != -1) {

                            tempoReg.add(tempo);
                            linhaVeh.add(all[a].split("\"")[1].substring(0, 3));
                            codVeh.add(all[a].split("\"")[1].substring(4));

                            velVeh.add(Double.valueOf(all[a].split("\"")[5]));

                            double pos = Double.valueOf(all[a].split("\"")[3]);
                            double ponto = (pos / mapa.getArestaExtensaoMETROS(indexAresta));
                            //System.out.println(ponto);

                            latVeh.add(mapa.getNode_lat()[mapa.getFromNodI(indexAresta)]
                                    + ponto * (mapa.getNode_lat()[mapa.getToNodI(indexAresta)] - mapa.getNode_lat()[mapa.getFromNodI(indexAresta)]));
                            lonVeh.add(mapa.getNode_lon()[mapa.getFromNodI(indexAresta)]
                                    + ponto * (mapa.getNode_lon()[mapa.getToNodI(indexAresta)] - mapa.getNode_lon()[mapa.getFromNodI(indexAresta)]));

                            /*System.out.println("Cadastrou "+linhaVeh.get(linhaVeh.size()-1)+"."+codVeh.get(codVeh.size()-1)+" em "
                                    + "t"+tempo+", velocidade "+velVeh.get(velVeh.size()-1)+"; aresta "+indexAresta+"; "+
                                           latVeh.get(latVeh.size()-1) + " "+lonVeh.get(latVeh.size()-1)); */
                        }
                    }
                }

            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

        String r = "Dump, processado, em " + horaAtual() + ", " + latVeh.size() + ", registros, de, posição, de, veículos\n"
                + "Tempo S, Dist M, linha V1, cod V1, lat V1, lon V1, veloc V1,  linha V2, num V2, lat V2, lon V2, veloc V2\n";
        //linha V1, num V1, lat V1, lon V1,  distancia(v1, v2),  linha V2, num V2, lat V2, lon V2
        System.out.print(r);
        int casos = 0;
        double dist;
        double t = 0.0;

        /*Graph graphx = new MultiGraph("URBS");
        for (int l = 0; l < linhas.size(); l++) 
            graphx.addNode(linhas.get(l));
        for (int l = 0; l < linhas.size(); l++) {
            for (int l2 = 0; l2 < linhas.size(); l2++)
                graphx.addEdge((String)linhas.get(l) + (String)linhas.get(l2), linhas.get(l), linhas.get(l2));
        }
        graphx.display();*/
        
        for (int a = 0; a < latVeh.size(); a++) {
            for (int bx = a + 1; bx < latVeh.size(); bx++) {
                //System.out.println(a+" e "+b+"; T "+tempoReg.get(a)+" T2 "+ tempoReg.get(b));

                if (tempoReg.get(a) > (comecoSeg + (30 * 60))) {
                    if (tempoReg.get(a).equals(tempoReg.get(bx))) {
                        dist = mapa.distanciaPontosMETROS(latVeh.get(a), lonVeh.get(a), latVeh.get(bx), lonVeh.get(bx));
                        //if(dist>0.0){
                        // System.out.println(a+" e "+bx+"; T "+tempoReg.get(a)+" T2 "+ tempoReg.get(bx)+" D: "+dist);
                        if (dist <= 300.0) {

                            r = r + tempoReg.get(a) + ", " + dist + ", "
                                    + linhaVeh.get(a) + ", " + codVeh.get(a) + ", " + latVeh.get(a) + ", " + lonVeh.get(a) + ", " + velVeh.get(a) + ", "
                                    + linhaVeh.get(bx) + ", " + codVeh.get(bx) + ", " + latVeh.get(bx) + ", " + lonVeh.get(bx) + ", " + velVeh.get(bx) + "\n";
                            casos++;
                            double hie;
                            String regE = linhaVeh.get(a)+"-"+linhaVeh.get(bx);
                            String rev = linhaVeh.get(bx)+"-"+linhaVeh.get(a);
                            if(!encontrosAnt.contains(regE) && !encontrosAnt.contains(rev)  ){ //se não estava no Tempo anterior, é um novo encontro
                                hie = tempoReg.get(a);
                                encontros = encontros + "inicio_t_"+(int)hie+"s "+regE+" \n";
                                contEncontros++;
                            }
                            encontrosAtual.add(regE); //mantem informado de onde tem encontro no segundo
                            
                            if (t != tempoReg.get(a)) {
                                
                                for(int z=0;z<encontrosAnt.size();z++)
                                    if(!encontrosAtual.contains(encontrosAnt.get(z)) &&  !encontrosAtual.contains(encontrosAnt.get(z).split("-")[1]+"-"+encontrosAnt.get(z).split("-")[0]))
                                        encontros = encontros + "fim_t_"+(int)t+"s "+encontrosAnt.get(z)+" \n";
                                
                               // try {
                               //     Thread.sleep(200);
                               // } catch (InterruptedException ex) {
                               //     Logger.getLogger(GeradorRotaSUMO.class.getName()).log(Level.SEVERE, null, ex);
                             //   }
                               // for(int e=0;e<graphx.getEdgeCount();e++)
                             //       graphx.getEdge(e).setAttribute("ui.color", 1);
                             
                                encontrosAnt = new ArrayList<>(); //mudou o T. Atualiza atual para anterior
                                for(int z=0;z<encontrosAtual.size();z++)
                                    encontrosAnt.add(encontrosAtual.get(z));
                                encontrosAtual = new ArrayList<>();
                                t = tempoReg.get(a);

                            }
                           //  graphx.getEdge((String)(linhaVeh.get(a)+(String)linhaVeh.get(bx))).setAttribute("ui.color", 0);*/
                            
                        }
                        //}
                    }
                }
            }
        }
        System.out.println("OK: Encontrou " + casos + " segundos de troca de informações!");
        emCsv(r, "arquivos SUMO\\output_posicoes");
        emTxt(encontros.substring(0, encontros.length()-2),"arquivos SUMO\\"+contEncontros+" encontros");

    }

    ArrayList<String> horCor = new ArrayList<>();
    ArrayList<Integer>[] departs;
    int[] intervalosMax;

    private void lerHorariosOnibus(String dia, String mes, String ano) {
        //[{"HORA":"07:10","PONTO":"TERMINAL BAIRRO ALTO","DIA":"1","NUM":"109120","TABELA":"1-1","ADAPT":"","COD":"340"}
        //0   1   2   3   4   5   6           7          8  9 10 1112 13 14  15   16  17   18 19 20  21 222324 25 26 27
        String file = "arquivos SUMO\\Urbs\\" + ano + "_" + mes + "_" + dia + "_tabelaLinha.json";

        departs = new ArrayList[linhas.size()];
        
        for (int x = 0; x < linhas.size(); x++) {
            departs[x] = new ArrayList<>();
        }

        System.out.println("PROC: Iniciando leitura de " + file + "! " + horaAtual());
        try {
            Scanner scanner;
            scanner = new Scanner(new File(file));
            scanner.useDelimiter("\\Z");
            file = scanner.next();

            String[] all = file.split("},");
            String[] sep;// = file.split("\"");

            for (int a = 0; a < all.length - 1; a++) {

                sep = all[a].split("\"");
                int tAtual = (Integer.valueOf(sep[3].split(":")[0]) * 3600 + Integer.valueOf(sep[3].split(":")[1]) * 60);
                int ind = linhas.indexOf(sep[27]);
                if (ind != -1 && sep[11].equals("2") && tAtual > comecoSeg && tAtual < fimSeg) {

                    if (ind == 3 && sep[7].equals("TERMINAL SITIO CERCADO") || (sep[7].equals("TERMINAL PINHEIRINHO") || sep[7].equals("TERMINAL PINHEIRINHO-PIRATINI"))) {

                        if (!departs[ind].contains(tAtual)) {
                            departs[ind].add(tAtual);
                            horLinha.add("r" + linhas.get(ind));
                            horSegundos.add(tAtual);
                            horCor.add(getCor(ind));
                        }

                    }

                }
            }

            for (int i = 0; i < linhas.size(); i++) {

                int[] aux = new int[departs[i].size()];
                for (int a = 0; a < aux.length; a++) {
                    int menor = 0;

                    for (int d = 0; d < departs[i].size(); d++) {
                        if (departs[i].get(d) < departs[i].get(menor)) {
                            menor = d;
                        }

                    }
                    aux[a] = departs[i].get(menor);
                    departs[i].remove(menor);
                }

                for (int a = 0; a < aux.length; a++) {
                    departs[i].add(aux[a]);
                    // System.out.print(aux[a]+" ");
                }
                // System.out.println("");
                /*intervalosMax[i] = 0;
                for (int d = 1; d < departs[i].size(); d++) {
                    if ((departs[i].get(d) - departs[i].get(d - 1)) > intervalosMax[i]) {
                        intervalosMax[i] = (departs[i].get(d) - departs[i].get(d - 1));
                    }
                }*/

                System.out.print(linhas.get(i) + ": " + departs[i].size() + "(" + intervalosMax[i] + " seg); ");

            }
            System.out.println("");

        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("OK: Leu " + horLinha.size() + " horarios de saida de terminais para as linhas! ");

    }

    private void lerDadosMovimentacaoBus(String dia, String mes, String ano) {
        //{"VEIC":"BA002","LAT":"-25.406573","LON":"-49.252543","DTHR":"25\/02\/2017 23:59:28","COD_LINHA":"211"}
        //0  1   2   3   4  5  6      7     8  9  10    11     12 13  14    15               16    17    18  19  20=0 
        String file = "arquivos SUMO\\Urbs\\" + ano + "_" + mes + "_" + dia + "_veiculos.json";
        codLinha = new ArrayList<>();
        codVeh = new ArrayList<>();
        segundos = new ArrayList<>();
        latV = new ArrayList<>();
        lonV = new ArrayList<>();
        veiculosHora = new ArrayList<>();
        intervalosMax = new int[linhas.size()];

        ArrayList<String> codLinha1 = new ArrayList<>();
        ArrayList<String> codVeh1 = new ArrayList<>();
        ArrayList<Integer> segundos1 = new ArrayList<>();
        ArrayList<Double> latV1 = new ArrayList<>();
        ArrayList<Double> lonV1 = new ArrayList<>();

       

        System.out.println("PROC: Iniciando leitura de " + file + "! " + horaAtual());
        try {
            Scanner scanner;
            scanner = new Scanner(new File(file));
            scanner.useDelimiter("VEIC");
            int c = 0;
            int linesToRead = 5000000;
            String[] all = new String[linesToRead-1];
            scanner.next();
            
            while (c < (linesToRead-1)){
                all[c] = scanner.next();
                c++;
            }

            int seg;
            cadLinha = new ArrayList<>();
           String[] sAux;
           String aux;
            String[] sep;// = file.split("\"");
            cores = new ArrayList<>();

            for (int a = 1; a < all.length; a++) {

                sep = all[a].split("\"");
                //System.out.println(sep[18]);
                if (linhas.contains(sep[18])) { //se linha é de interesse

                    aux = sep[14].replace("\\/", ".").replace(" ", ":");
                    //25.02.2017:23:59:28
                    sAux = aux.split(":");
                    seg = Integer.valueOf(sAux[3]) + Integer.valueOf(sAux[2]) * 60 + Integer.valueOf(sAux[1]) * 3600;

                    if (seg > comecoSeg && seg < fimSeg) { //se for do intervalo de tempo de interesse

                        if (cadLinha.contains(sep[18])) {  //se a linha já foi cadastrada
                           // if (cadVeiculo.get(cadLinha.indexOf(sep[18])).equals(sep[2])) { // se já cadastrada, precisa ser o mesmo veículo (só faz track do primeiro veiculo)
                                codVeh1.add(sep[2]);
                                codLinha1.add(sep[18]);
                                latV1.add(Double.valueOf(sep[6]));
                                lonV1.add(Double.valueOf(sep[10]));
                                segundos1.add(seg);

                           // }

                        } else {  //caso a linha ainda nao tenha sido cadastrada
                            cadLinha.add(sep[18]);
                            //cadVeiculo.add(sep[2]);
                            cores.add(this.getCor(cadLinha.size() - 1));

                            codVeh1.add(sep[2]);
                            codLinha1.add(sep[18]);
                            latV1.add(Double.valueOf(sep[6]));
                            lonV1.add(Double.valueOf(sep[10]));
                            segundos1.add(seg);

                        }

                    } //fim SE é no intervalo de tempo de interesse
                } //fim SE linha é de interesse

            }

            System.out.println("OK: Leu todas as " + codVeh1.size() + " localizações! Iniciando inversão, para ficar em ordem cronologica..." + horaAtual());

            for (int x = codVeh1.size() - 1; x >= 0; x--) {
                codVeh.add(codVeh1.get(x));
                codLinha.add(codLinha1.get(x));
                latV.add(latV1.get(x));
                lonV.add(lonV1.get(x));
                segundos.add(segundos1.get(x));
            }

            System.out.println("OK: " + codVeh.size() + " Localizações em ordem cronologica! " + horaAtual());

            
             ArrayList<String> cadVeiculo = new ArrayList<>();
             ArrayList<Integer> inicioSeg = new ArrayList<>();
             ArrayList<Double> latInicial = new ArrayList<>();
              ArrayList<Double> lonInicial = new ArrayList<>();
              int ind;
            
              ArrayList<Integer> voltaSeg = new ArrayList<>();
              ArrayList<String> voltaLinha = new ArrayList<>();
              
            for(int l=0;l<linhas.size();l++){
                intervalosMax[l]= 99999;
                for (int x = 0; x < codVeh.size() - 1; x++) {
                    if(codLinha.get(x).equals(linhas.get(l))){
                        if (cadVeiculo.contains(codVeh.get(x))){//se já tem veiculo cadastrado
                            ind = cadVeiculo.indexOf(codVeh.get(x));
                            //se está a X segundos depois
                            if(inicioSeg.get(ind)<segundos.get(x)+900)
                            //se está a X metros do local inicial
                                if(mapa.distanciaPontosMETROS(latV.get(x), lonV.get(x), latInicial.get(ind), lonInicial.get(ind))<15){
                            //linha tem um tempo de "volta".
                                    voltaLinha.add(codLinha.get(x));
                                    voltaSeg.add(segundos.get(x)-inicioSeg.get(ind));
                                    cadVeiculo.remove(ind);
                                    inicioSeg.remove(ind);
                                    latInicial.remove(ind);
                                    lonInicial.remove(ind);
                                    
                                    if(voltaSeg.get(voltaSeg.size()-1)>1000 && voltaSeg.get(voltaSeg.size()-1)<intervalosMax[l])
                                        intervalosMax[l]=voltaSeg.get(voltaSeg.size()-1);
                                }
                    
                        }else{
                        // caso contrario
                        cadVeiculo.add(codVeh.get(x));
                        inicioSeg.add(segundos.get(x));
                        latInicial.add(latV.get(x));
                        lonInicial.add(lonV.get(x));                       
                        }
                    }
                }
                
                System.out.print(linhas.get(l)+": ");
                for(int v=0;v<voltaSeg.size();v++){
                    if(voltaLinha.get(v).equals(linhas.get(l)))
                        System.out.print(voltaSeg.get(v)+" ");
                }
                System.out.println("");
                
            }
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void loadOSMData() {

        try {

            try (FileInputStream arquivoLeitura = new FileInputStream(OSMFileLocation.replace("osm", "dat"))) {
                ObjectInputStream objLeitura = new ObjectInputStream(arquivoLeitura);

                mapa = (Mapping) objLeitura.readObject();
                mapa.setROIsizeM(1000);

                objLeitura.close();

            }
            System.out.println("OK: Recuperou objeto mapa de '" + OSMFileLocation + ".dat'! (" + mapa.getFromNodI().size() + " arestas, " + mapa.getContNodes() + " nodes, " + mapa.getWayCount() + " ways) " + horaAtual() + "\n");
            return;
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            System.out.println("ALERT: Não recuperou registros de arquivo '" + OSMFileLocation.replace("osm", "dat") + "'");

        }

        System.out.println("PROC: Recuperando dados geográficos do xml OSM... " + horaAtual());
        //Scanner scanner;

        /*try {
            scanner = new Scanner(new File(OSMFileLocation));
            scanner.useDelimiter("\\Z");*/
            mapa = new Mapping(OSMFileLocation, false, 1000, 0.2, SUMOXmlLocation);
            //mapa.criarArestasDirecionadas();

        /*} catch (FileNotFoundException ex) {
            Logger.getLogger(TaxiODSolver.class.getName()).log(Level.SEVERE, null, ex);
        }*/

        System.out.println("END: Recuperou dados e criou objetos geográficos! " + horaAtual());

        try {
            FileOutputStream arquivoGrav = new FileOutputStream(OSMFileLocation.replace(".osm", ".dat"));
            ObjectOutputStream objGravar = new ObjectOutputStream(arquivoGrav);
            objGravar.writeObject(mapa);
            objGravar.flush();
            objGravar.close();
            arquivoGrav.flush();
            arquivoGrav.close();
            System.out.println("OK: Mapa '" + OSMFileLocation + "' salvo em .dat! " + horaAtual());
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("ERROR: Falha ao salvar .dat de " + OSMFileLocation);
        }

    }

    public static String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));
    }

    public void salvar(String content, String fileName) {

        criaDiretorio("arquivos SUMO");
        //  emTxt(content,fileName);
        emXml(content, "arquivos SUMO\\" + fileName);

    }

    private void emTxt(String content, String fileName) {
        try {
            File file = new File(fileName + ".txt");

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            // j.salvou(true);
            // System.out.println("Salvou " + fileName + ".txt");

        } catch (IOException e) {
            //j.salvou(false);
            System.out.println("Falha para" + fileName + ".txt");
            e.printStackTrace();
        }
    }

    private void emCsv(String content, String fileName) {
        try {
            File file = new File(fileName + ".csv");

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            // j.salvou(true);
            // System.out.println("Salvou " + fileName + ".txt");

        } catch (IOException e) {
            //j.salvou(false);
            System.out.println("Falha para" + fileName + ".csv");
            e.printStackTrace();
        }
    }

    private void emXml(String content, String fileName) {
        try {
            File file = new File(fileName + ".xml");

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            // j.salvou(true);
            System.out.println("Salvou " + fileName + ".xml");

        } catch (IOException e) {
            //j.salvou(false);
            System.out.println("Falha para" + fileName + ".xml");
            e.printStackTrace();
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

    public String getCor(int n) {

        n = n % 9;
        
        switch (n) {
            case 0:
                return "red";
            case 1:
                return "blue";
            case 2:
                return "yellow";
            case 3:
                return "white";
            case 4:
                return "purple";
            case 5:
                return "green";
            case 6:
                return "gray";
            case 7:
                return "pink";
            case 8:
                return "orange";
            default:
                return Math.random() + ", " + Math.random() + ", " + Math.random();

        }

    }

}
