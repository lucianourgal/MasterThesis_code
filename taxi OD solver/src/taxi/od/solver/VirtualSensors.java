
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package taxi.od.solver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Luciano
 */
public class VirtualSensors implements Serializable {

    //private int[][] contagem;
    //private boolean[] node_sensor;
    //private boolean[] node_pode_ser_sensor;
    //private final boolean[] oneWay;
    private int discretTemporal;
    private final int minInterv;
    private final int registrosPorMinuto;
    private String name;

    private ArrayList<String> fromNod;
    private ArrayList<String> toNod;

    private ArrayList<Integer> fromNodI;
    private ArrayList<Integer> toNodI;

    private ArrayList<String> arestaKind;

    private int contAresta[][];
    private int contArestaBatch[][][];
    private double arestaVariance[][];

    private short batchSize;
    private int[] indiceArestKind;
    DecimalFormat df2 = new DecimalFormat(".##");

    public int getContArestaBatch(int aresta, int tempo, int batch) {
        return contArestaBatch[aresta][tempo][batch];
    }

    public void lerArquivoVolumesNYC(Mapping map, String file) {
        //ID	Segment ID  Roadway Name      From	To	      Direction	Date	  12:00-1:00 AM	  1:00-2:00AM	[...]
        //1	2153	H   UGUENOT AVE	     WOODROW 	STAFFORD AVE    NB	02/02/13	106	74	45	[...]
        //0      1              2             3            4             5          6          7             8       9   [...]   36
        discretTemporal = 24;
        ArrayList<String> direction = new ArrayList<>();
        ArrayList<String> data = new ArrayList<>();
        ArrayList<String> diasDif = new ArrayList<>();

        ArrayList<String> ruas = new ArrayList<>();
        ArrayList<Integer> wayDaRua;
        ArrayList<Integer> aresta = new ArrayList<>();

        ArrayList<Double>[] counts;

        String[] all;
        String[] sep;

        try {
            Scanner scanner;
            scanner = new Scanner(new File(file));
            scanner.useDelimiter("\\Z");
            file = scanner.next();
            all = file.split("\n");
            counts = new ArrayList[all.length];

            for (int a = 1; a < all.length; a++) {
                sep = all[a].split(",");

                data.add(sep[6].replace(" ", ""));
                if (!diasDif.contains(data.get(data.size() - 1))) {
                    diasDif.add(sep[6].replace(" ", ""));
                }

                direction.add(sep[6]);

                ruas.add(sep[2]);
                ruas.add(sep[3]);
                ruas.add(sep[4]);

                counts[a - 1] = new ArrayList<>();
                for (int c = 0; c < 24; c++) {
                    counts[a - 1].add(Double.valueOf(sep[7 + c]));
                }

            }
            batchSize = (short) diasDif.size();
            wayDaRua = map.encontrarIndexWaysPorNomeDeRua(ruas);

            int arestaOk = 0;
            //encontrar arestas Mapping
            for (int d = 0; d < direction.size(); d++) {
                aresta.add(map.calcIndexArestaDatasetNYC(wayDaRua.get(d * 3), wayDaRua.get(d * 3 + 1), wayDaRua.get(d * 3 + 2), direction.get(d)));
                if (aresta.get(aresta.size() - 1) >= 0) {
                    arestaOk++;
                }
            }

            contAresta = new int[getFromNodCod().size()][discretTemporal + 1];
            contArestaBatch = new int[getFromNodCod().size()][discretTemporal + 1][batchSize];
            arestaVariance = new double[getFromNodCod().size()][discretTemporal + 1];

            for (int ar = 0; ar < aresta.size(); ar++) {
                if (aresta.get(ar) > -1) {
                    for (int c = 0; c < counts[ar].size(); c++) {
                        contArestaBatch[aresta.get(ar)][c][diasDif.indexOf(data.get(ar))] = (int) (double) counts[ar].get(c);
                        contAresta[aresta.get(ar)][c] += counts[ar].get(c);
                    }
                }
            }

            System.out.println("OK: " + diasDif.size() + " batches, " + (aresta.size() / diasDif.size()) + " sensores/dia. Destes, " + arestaOk + " arestas identificadas.");

        } catch (FileNotFoundException e) {
            System.out.println("ERROR: Não achou arquivo " + file + "! (virtualSensors.lerArquivoVolumesNYC)");
        }

    }

    public void gerarScriptLocaisMovimentados(Mapping map) {

        System.out.println("OK: Do nothing.");

    }

    int[] mov2;
    int[] arestMov;

    public int[] getArestasMaismovimentadasIndexARESTA() {
        return arestMov;
    }

    public int[] getArestasMaismovimentadasIndexTONODE() {
        return mov2;
    }

    int arestasSensorGAMM = -1;
    int tempoPrioriGAMM = -1;
    int[] mov;

    //encontrando arestas mais mov
    public int[] getArestasMaismovimentadasIndex(int n, int t) { //GAMM function

        int maiorZero = 0;
        double med = 0;
        
        if (n == -1) {
            n = getFromNodIndex().size();
        }
        if(n==0){
            System.out.println("ERROR: n==0 (virtualSensors.getArestasMaisMovimentadasIndex)");
        }
        
        /*if (n == arestasSensorGAMM && tempoPrioriGAMM == t  && mov.length==n) {
            System.out.println("INFO: Return antecipado, n=arestasSensorGAMM="+n+" e t=tempoPrioriGAMM="+t+"; mov.lenght="+mov.length+" (VirtualSensors.gerArestaMaisMovimentadasIndex)");
            return mov;
        }*/

        mov = new int[n];
        arestMov = new int[n];
        mov2 = new int[n];
        int menorInd;

        for (int n2 = 0; n2 < n; n2++) {
            mov[n2] = -1;
        }

        for (int a = 0; a < fromNod.size(); a++) { //de aresta em aresta

            menorInd = getIndexArestaMenosMovVetMOV(t);
            if (mov[menorInd] == -1) {
                //ocup++; //ainda está preenchendo o vetor
                mov[menorInd] = a;
            } else if (getContArestaArestaT(a, t) > getContArestaArestaT(mov[menorInd], t)) {
                mov[menorInd] = a;
            }

        } //fim laço aresta

        int cont = 0;

        for (int n2 = 0; n2 < n; n2++) {

            if (mov[n2] == -1) {
                System.out.println("ERRO: return prematuro de mov[]: mov[" + n2 + "]==-1  (virtualSensors.getArestasMaisMovIndex)");
                return mov;
            }
            
            if(getContArestaArestaT(mov[n2], t)>0){
                maiorZero++;
                med = med + getContArestaArestaT(mov[n2], t);
            }
           
            arestMov[n2] = mov[n2];
            mov[n2] = fromNodI.get(mov[n2]); //transforma index de aresta em index de node
            mov2[n2] = toNodI.get(arestMov[n2]);

            //    System.out.println("(cont = "+getContArestaTotalAresta(mov[n2])+") Add fromNodI.get(mov["+n2+"]) = fromNodI.get("+mov[n2]+"]) = " + fromNodI.get(mov[n2]));    
        }

        for (int n1 = 0; n1 < n; n1++) {
            for (int n2 = n1 + 1; n2 < n; n2++) {
                if (arestMov[n2] == arestMov[n1] && n2 != n1) {
                    System.out.println("ALERT: " + ++cont + " Repetição em vetor de arestas mais movimentadas [" + n2 + " e " + (n2 - 1) + "] " + mov[n2] + " = " + mov[n2 - 1] + " - virtualSensors.getArestasMaismovimentadasIndex");
                }
            }
        }
        System.out.println("OK: "+arestMov.length + " sensores/arestas, "+maiorZero+" >0, media fluxo "+med/maiorZero);
        arestasSensorGAMM = n;
        tempoPrioriGAMM = t;
        this.salvarDat(name);
        return mov;
    }

    public int getIndexArestaMenosMovVetMOV(int temp) {
        double menor = 80000.0;
        int menorInd = -1;

        if (mov.length == 0) {
            System.out.println("ERROR: mov.lenght = 0 (virtualSensors.getArestaMenosMov)");
        }

        for (int m = 0; m < mov.length; m++) {
            if (mov[m] == -1) //encontrou posição vaga no vetor
            {
                return m;
            } else if (getContArestaArestaT(mov[m], temp) < menor) {
                menorInd = m;
                menor = getContArestaArestaT(mov[m], temp);
            }
        }
        return menorInd;
    }

    
    //encontrando arestas com mais fluxo/rotas
    public int[] getArestasMaismovimentadasMenosRotasIndex(int n, int t, ODMatrix odm) { //GAMM function

        int maiorZero = 0;
        double med = 0;
        
        if (n == -1) {
            n = getFromNodIndex().size();
        }
        if(n==0){
            System.out.println("ERROR: n==0 (virtualSensors.getArestasMaisMovimentadasIndex)");
        }
        
        /*if (n == arestasSensorGAMM && tempoPrioriGAMM == t  && mov.length==n) {
            System.out.println("INFO: Return antecipado, n=arestasSensorGAMM="+n+" e t=tempoPrioriGAMM="+t+"; mov.lenght="+mov.length+" (VirtualSensors.gerArestaMaisMovimentadasIndex)");
            return mov;
        }*/

        mov = new int[n];
        arestMov = new int[n];
        mov2 = new int[n];
        int menorInd;

        for (int n2 = 0; n2 < n; n2++) {
            mov[n2] = -1;
        }

        for (int a = 0; a < fromNod.size(); a++) { //de aresta em aresta

            menorInd = getIndexArestaMenosMovVetMOV(t, odm);
            if (mov[menorInd] == -1) {
                //ocup++; //ainda está preenchendo o vetor
                mov[menorInd] = a;
            } else if     (getContFluxoPorRota(a, t, odm) >  getContFluxoPorRota(mov[menorInd], t, odm)){// getContArestaArestaT(mov[menorInd], t)) {
                mov[menorInd] = a;
            }

        } //fim laço aresta

        int cont = 0;

        for (int n2 = 0; n2 < n; n2++) {

            if (mov[n2] == -1) {
                System.out.println("ERRO: return prematuro de mov[]: mov[" + n2 + "]==-1  (virtualSensors.getArestasMaisMovIndex)");
                return mov;
            }
            
            if(getContFluxoPorRota(mov[n2], t, odm)>0){//if(getContArestaArestaT(mov[n2], t)>0){
                maiorZero++;
                med = med + getContFluxoPorRota(mov[n2], t, odm);
            }
           
            arestMov[n2] = mov[n2];
            mov[n2] = fromNodI.get(mov[n2]); //transforma index de aresta em index de node
            mov2[n2] = toNodI.get(arestMov[n2]);

            //    System.out.println("(cont = "+getContArestaTotalAresta(mov[n2])+") Add fromNodI.get(mov["+n2+"]) = fromNodI.get("+mov[n2]+"]) = " + fromNodI.get(mov[n2]));    
        }

        for (int n1 = 0; n1 < n; n1++) {
            for (int n2 = n1 + 1; n2 < n; n2++) {
                if (arestMov[n2] == arestMov[n1] && n2 != n1) {
                    System.out.println("ALERT: " + ++cont + " Repetição em vetor de arestas mais movimentadas [" + n2 + " e " + (n2 - 1) + "] " + mov[n2] + " = " + mov[n2 - 1] + " - virtualSensors.getArestasMaismovimentadasIndex");
                }
            }
        }
        System.out.println("OK: "+arestMov.length + " sensores/arestas, "+maiorZero+" >0, media fluxo "+med/maiorZero);
        arestasSensorGAMM = n;
        tempoPrioriGAMM = t;
        this.salvarDat(name);
        return mov;
    }

    public int getIndexArestaMenosMovVetMOV(int temp, ODMatrix odm) {
        double menor = 80000.0;
        int menorInd = -1;

        if (mov.length == 0) {
            System.out.println("ERROR: mov.lenght = 0 (virtualSensors.getArestaMenosMov)");
        }

        for (int m = 0; m < mov.length; m++) {
            if (mov[m] == -1) //encontrou posição vaga no vetor
            {
                return m;
            } else if ((getContFluxoPorRota(mov[m], temp, odm)) < menor) {
                menorInd = m;
                menor = getContFluxoPorRota(mov[m], temp, odm);
            }
        }
        return menorInd;
    }
    
    private double getContFluxoPorRota(int a, int t, ODMatrix odm){
    
        
        if(odm.getContRotasPorAresta(a, t, this.fromNodI.size())>0)
            return  (getContArestaArestaT(a, t)*odm.getContRotasPorAresta(a, t, this.fromNodI.size()));
        else
            return 0;
    }
    
    
    
    //encontrando arestas com mais rotas
     public int[] getArestasMaisRotasIndex(ODMatrix odm, int n, int t) { //GAMM function

        int maiorZero = 0;
        double med = 0;
        
        if (n == -1) {
            n = getFromNodIndex().size();
        }
        if(n==0){
            System.out.println("ERROR: n==0 (virtualSensors.getArestasMaisRotasIndex)");
        }
        
        /*if (n == arestasSensorGAMM && tempoPrioriGAMM == t  && mov.length==n) {
            System.out.println("INFO: Return antecipado, n=arestasSensorGAMM="+n+" e t=tempoPrioriGAMM="+t+"; mov.lenght="+mov.length+" (VirtualSensors.getArestaMaisRotas)");
            return mov;
        }*/

        mov = new int[n];
        arestMov = new int[n];
        mov2 = new int[n];
        int menorInd;

        for (int n2 = 0; n2 < n; n2++) {
            mov[n2] = -1;
        }

        for (int a = 0; a < fromNod.size(); a++) { //de aresta em aresta

            menorInd = getIndexArestaMenosRotas(odm,t);
            if (mov[menorInd] == -1) {
                //ocup++; //ainda está preenchendo o vetor
                mov[menorInd] = a;
            } else if (odm.getContRotasPorAresta(a, t, this.fromNodI.size()) > odm.getContRotasPorAresta(mov[menorInd], t,this.fromNodI.size())) {
                mov[menorInd] = a;
            }

        } //fim laço aresta

        int cont = 0;

        for (int n2 = 0; n2 < n; n2++) {

            if (mov[n2] == -1) {
                System.out.println("ERRO: return prematuro de mov[]: mov[" + n2 + "]==-1  (virtualSensors.getArestasMaisMovIndex)");
                return mov;
            }
            
            if(odm.getContRotasPorAresta(mov[n2], t, this.fromNodI.size())>0){
                maiorZero++;
                med = med + odm.getContRotasPorAresta(mov[n2], t, this.fromNodI.size());
            }
           
            arestMov[n2] = mov[n2];
            mov[n2] = fromNodI.get(mov[n2]); //transforma index de aresta em index de node
            mov2[n2] = toNodI.get(arestMov[n2]);
 
        }

        for (int n1 = 0; n1 < n; n1++) {
            for (int n2 = n1 + 1; n2 < n; n2++) {
                if (arestMov[n2] == arestMov[n1] && n2 != n1) {
                    System.out.println("ALERT: " + ++cont + " Repetição em vetor de arestas mais rotas [" + n2 + " e " + (n2 - 1) + "] " + mov[n2] + " = " + mov[n2 - 1] + " - virtualSensors.getArestasMaisRotasIndex");
                }
            }
        }
        System.out.println("OK: "+arestMov.length + " arestas, "+maiorZero+" >0 rotas, media rotas "+med/maiorZero);
        arestasSensorGAMM = n;
        tempoPrioriGAMM = t;
        this.salvarDat(name);
        return mov;
    }

     
    //encontrando arestas com menos rotas
     public int[] getArestasMenosRotasIndex(ODMatrix odm, int n, int t) { //GAMM function

        int maiorZero = 0;
        double med = 0;
        
        if (n == -1) {
            n = getFromNodIndex().size();
        }
        if(n==0){
            System.out.println("ERROR: n==0 (virtualSensors.getArestasMenosRotasIndex)");
        }
        
        /*if (n == arestasSensorGAMM && tempoPrioriGAMM == t  && mov.length==n) {
            System.out.println("INFO: Return antecipado, n=arestasSensorGAMM="+n+" e t=tempoPrioriGAMM="+t+"; mov.lenght="+mov.length+" (VirtualSensors.getArestaMenosRotas)");
            return mov;
        }*/

        mov = new int[n];
        arestMov = new int[n];
        mov2 = new int[n];
        int maiorInd;

        for (int n2 = 0; n2 < n; n2++) {
            mov[n2] = -1;
        }

        for (int a = 0; a < fromNod.size(); a++) { //de aresta em aresta

            if (odm.getContRotasPorAresta(a, t, this.fromNodI.size()) > 0) {
            
            maiorInd = getIndexArestaMaisRotas(odm,t);
            if (mov[maiorInd] == -1) {
                //ocup++; //ainda está preenchendo o vetor
                mov[maiorInd] = a;
            } else if (odm.getContRotasPorAresta(a, t, this.fromNodI.size()) < odm.getContRotasPorAresta(mov[maiorInd], t,this.fromNodI.size())) {
                mov[maiorInd] = a;
            }

        }
            
        } //fim laço aresta

        int cont = 0;

        for (int n2 = 0; n2 < n; n2++) {

            if (mov[n2] == -1) {
                System.out.println("ERRO: return prematuro de mov[]: mov[" + n2 + "]==-1  (virtualSensors.getArestaMenosRotas)");
                return mov;
            }
            
            if(odm.getContRotasPorAresta(mov[n2], t, this.fromNodI.size())>0){
                maiorZero++;
                med = med + odm.getContRotasPorAresta(mov[n2], t, this.fromNodI.size());
            }
           
            arestMov[n2] = mov[n2];
            mov[n2] = fromNodI.get(mov[n2]); //transforma index de aresta em index de node
            mov2[n2] = toNodI.get(arestMov[n2]);
 
        }

        for (int n1 = 0; n1 < n; n1++) {
            for (int n2 = n1 + 1; n2 < n; n2++) {
                if (arestMov[n2] == arestMov[n1] && n2 != n1) {
                    System.out.println("ALERT: " + ++cont + " Repetição em vetor de arestas mais rotas [" + n2 + " e " + (n2 - 1) + "] " + mov[n2] + " "
                            + "= " + mov[n2 - 1] + " - virtualSensors.getArestasMenosRotasIndex");
                }
            }
        }
        System.out.println("OK: "+arestMov.length + " arestas, "+maiorZero+" >0 rotas, media rotas "+med/maiorZero);
        arestasSensorGAMM = n;
        tempoPrioriGAMM = t;
        this.salvarDat(name);
        return mov;
    } 
     
     
    public int getIndexArestaMenosRotas(ODMatrix odm, int temp) {
        double menor = 80000.0;
        int menorInd = -1;

        if (mov.length == 0) {
            System.out.println("ERROR: mov.lenght = 0 (virtualSensors.getArestaMenosRotas)");
        }
        
        for (int m = 0; m < mov.length; m++) {
            if (mov[m] == -1) //encontrou posição vaga no vetor
            {
                return m;
            } else if (odm.getContRotasPorAresta(mov[m], temp, this.fromNodI.size()) < menor) {
                menorInd = m;
                menor = odm.getContRotasPorAresta(mov[m], temp, this.fromNodI.size());
            }
        }
        return menorInd;
    }
   
        public int getIndexArestaMaisRotas(ODMatrix odm, int temp) {
        double maior = -1;
        int maiorInd = -1;

        if (mov.length == 0) {
            System.out.println("ERROR: mov.lenght = 0 (virtualSensors.getArestaMaisRotas)");
        }
        
        for (int m = 0; m < mov.length; m++) {
            if (mov[m] == -1) //encontrou posição vaga no vetor
            {
                return m;
            } else if (odm.getContRotasPorAresta(mov[m], temp, this.fromNodI.size()) > maior) {
                maiorInd = m;
                maior = odm.getContRotasPorAresta(mov[m], temp, this.fromNodI.size());
            }
        }
        return maiorInd;
    }
    
    
    
    public int getFromNodIndex(int ar) {
        return fromNodI.get(ar);
    }

    public int getToNodIndex(int ar) {
        return toNodI.get(ar);
    }

    public int[][] getContArestaMatrix() {

        return contAresta;

    }

    public int getContArestaArestaT(int arest, int t) {

        if (t == -1) {
            return getContArestaTotalAresta(arest);
        }

        return contAresta[arest][t];

    }

    public int getContArestaTotalAresta(int arest) {
        int sum = 0;

        for (int a = 0; a < discretTemporal; a++) {
            sum = sum + contAresta[arest][a];
        }

        return sum;
    }

    public int getContArestaMomento(int arest, int temp) {
        return contAresta[arest][temp];
    }

    public void criarArestasDirecionadas(Mapping map, int batchSize) {

        System.out.println("PROC: Cadastrando arestas...  (VirtualSensors)");

        this.batchSize = (short) batchSize;
        //ArrayList<String>[] way_nodes = map.getWay_nodes();
        //boolean[] oneWay = map.getOneWay();
        //String[] way_kind = map.getWayKind();

        fromNodI = map.getFromNodI();
        toNodI = map.getToNodI();
        arestaKind = map.getArestaKindVet();
        fromNod = map.getFromNod();
        toNod = map.getToNod();
        
        if(fromNodI.size()!=fromNod.size())
            System.out.println("ERROR: fromNodI.size()!=fromNod.size() - (VirtualSensors.criarArestasDirecionadas)");

        /* for (int w = 0; w < map.getWayCount(); w++) { //de way em way
            addArestasDoWay(way_nodes[w], oneWay[w], way_kind[w], map); //adiciona arestas deste way
        }*/
        indiceArestKind = new int[fromNod.size()];

        contAresta = new int[map.getFromNod().size()][discretTemporal + 1];
        contArestaBatch = new int[map.getFromNod().size()][discretTemporal + 1][batchSize];
        arestaVariance = new double[map.getFromNod().size()][discretTemporal + 1];

        for (int a = 0; a < map.getFromNod().size(); a++) {
            for (int b = 0; b < discretTemporal; b++) {
                contAresta[a][b] = 0;
                arestaVariance[a][b] = -1;
                for (int bat = 0; bat < batchSize; bat++) {
                    contArestaBatch[a][b][bat] = 0;
                }
            }
        }

        System.out.println("OK: Cadastrou " + getFromNodCod().size() + " arestas! ");

    }

    public void calcVarianciaArestas() {

        for (int a = 0; a < getFromNodCod().size(); a++) {
            for (int b = 0; b < discretTemporal; b++) {
                arestaVariance[a][b] = varianciaS(contArestaBatch[a][b]);
            }
        }
    }

    public double getArestaVariance(int aresta, int tempo) {
        if (arestaVariance[aresta][tempo] > 0.15) {
            return arestaVariance[aresta][tempo];
        } else {
            return 0.12;
        }
    }

    public String getArestaKind(int ind) {

        if (ind < arestaKind.size()) {
            return arestaKind.get(ind);
        } else {
            System.out.println("ERROR: Buscando arestaKind além dos limites (virtualSensors.GetArestaKind)");
        }

        return "null";

    }

    public void setIndiceArestaKind(int indAresta, int indKind) {

        this.indiceArestKind[indAresta] = indKind;

    }

    public int getIndiceArestaKind(int indAresta) {

        return indiceArestKind[indAresta];

    }

    /* private void addArestasDoWay(ArrayList<String> way_node, boolean oneWay, String kind, Mapping map) {
        for (int x = 0; x < way_node.size() - 1; x++) {
            fromNod.add(way_node.get(x));
            toNod.add(way_node.get(x + 1));
            arestaKind.add(kind);
            fromNodI.add(map.getNodeIndex(way_node.get(x)));
            toNodI.add(map.getNodeIndex(way_node.get(x + 1)));
            if (!oneWay) {
                fromNod.add(way_node.get(x + 1));
                toNod.add(way_node.get(x));
                arestaKind.add(kind);
                fromNodI.add(map.getNodeIndex(way_node.get(x + 1)));
                toNodI.add(map.getNodeIndex(way_node.get(x)));
            }
        }
    }*/
    public VirtualSensors(int discretTemporal, Mapping mapa, int registrosPorMinut, String name1) {

        registrosPorMinuto = registrosPorMinut;
        name = name1;
        //oneWay = oneW;
        this.discretTemporal = discretTemporal;
        minInterv = (24 * 60) / discretTemporal;
        
        System.out.println("OK: Objeto virtualSensors com discretTemp = "+discretTemporal+"; "+registrosPorMinut+" registros por min, "+minInterv+" min interval - "+name1);

        //  numSensores = (int) (contNodes * coberturaSensores);
        // node_sensor = new boolean[contNodes];
        // node_pode_ser_sensor = new boolean[contNodes];
        // contagem = new int[contNodes][discretTemporal+1];
        //  int contNodesPodeSerSen =0;
        /*  
      int contx =0;
        
        System.out.println("Inicializando objeto VirtualSensors...");
        
        for (int a = 0; a < contNodes; a++) {
        //    node_sensor[a] = false;
            for (int b = 0; b < discretTemporal; b++) {
                contagem[a][b] = 0;
            }
            
            //System.out.println(node_pode_ser_sensor.length+" "+ oneWay.length +" " + nodes.length);
            //node_pode_ser_sensor se: oneWay e ApenasEmUmWay
           // node_pode_ser_sensor[a] = (mapa.nodeComApenasUmWay(a) && mapa.wayDoNodeEhOneWay(a));
          
         //   if(node_pode_ser_sensor[a])
           //     contNodesPodeSerSen++;
            
           
            if(contx==25000){
            System.out.println("Node "+a+"/"+contNodes+" ok");
            contx=0;
            }
             contx++;
        } */
        //  System.out.println("RSLT: Apenas "+contNodesPodeSerSen+" / "+contNodes+" podem ser usados como sensores.");
    }

    public void zerarInfs() {

        contNotFound = 0;

        for (int a = 0; a < getFromNodCod().size(); a++) {
            for (int b = 0; b < discretTemporal; b++) {
                contAresta[a][b] = 0;
                for (int bat = 0; bat < batchSize; bat++) {
                    contArestaBatch[a][b][bat] = 0;
                }
            }
        }

        contFound = 0;
    }

    int contNotFound = 0;
    int contFound = 0;

    public int distribuirPelosNodes(ArrayList<Integer> nodes, Timestamp tempo, int bat) {
        //  contNotFound = 0;
        int min = tempo.getHours() * 60 + tempo.getMinutes();
        int t;// = (int) (min / discretTemporal);
        int o = 0;

        if (registrosPorMinuto == 0) {
            System.out.println("ERROR: registrosPorMinuto == 0, divisão por zero. (virtualSensors)");
        }
        if (minInterv == 0) {
            System.out.println("ERROR: minInterv == 0, divisão por zero. (virtualSensors)");
        }

        for (int a = 0; a < nodes.size() - 1; a++) {

            /*
            int min = tempo.getHours() * 60 + tempo.getMinutes();
            int t = (int) (min / minInterv);
             */
            t = (int) ((min + (a / registrosPorMinuto)) / minInterv);
            // System.out.println(nodes.get(a)+" "+t);
            //o += addCont(node_id[nodes.get(a)], node_id[nodes.get(a + 1)], t, bat);
            o += addContREDUX(nodes.get(a), nodes.get(a + 1), t, bat);

        }

        if (o > 0) {
            return o / o;
        } else {
            return 0;
        }

    }

    public void printStatsVariancia(int tempo, int QprimeirasArestas) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        getArestasMaismovimentadasIndex(QprimeirasArestas, -1);
        //int [] indAr = getArestasMaismovimentadasIndexARESTA(); USE arestMov

        for (int c = 0; c < arestMov.length; c++) {
            stats.addValue(arestaVariance[arestMov[c]][tempo]);
        }

        System.out.println("STATS: arestasVariance (t" + tempo + "). "
                + "Min: " + df2.format(stats.getMin()) + "; perc25: " + df2.format(stats.getPercentile(25)) + "; "
                + "mean: " + df2.format(stats.getMean()) + "; perc50: " + df2.format(stats.getPercentile(50)) + "; "
                + "perc75: " + df2.format(stats.getPercentile(75)) + "; max: " + df2.format(stats.getMax()) + ";");

    }

    public void printContFound() {

        System.out.println("INF: Arestas de registros localizadas = " + contFound + " / " + (contFound + contNotFound) + ". (" + ((double) contFound * 100.0) / (double) (contFound + contNotFound) + "%)");

    }

    private int addCont(String from, String to, int t, int batch) {

        //passa por todas as arestas cadastradas
        for (int a = 0; a < getToNodCod().size(); a++) {

            //se encontrar correspondente
            if (getFromNodCod().get(a).equals(from) && getToNodCod().get(a).equals(to)) {

                //aumenta cont da aresta, de encontrados, e encerra loop.
                contAresta[a][t]++;
                contArestaBatch[a][t][batch]++;
                contFound++;
                return 1;
            }

        }
        contNotFound++;
        return 0;
    }

    public int addContREDUX(int from, int to, int t, int batch) {

        //passa por todas as arestas cadastradas
        for (int a = 0; a < toNodI.size(); a++) {

            //se encontrar correspondente
            if (fromNodI.get(a) == from) {
                if (toNodI.get(a) == to) {

                    //aumenta cont da aresta, de encontrados, e encerra loop.
                    contAresta[a][t]++;
                    contArestaBatch[a][t][batch]++;
                    contFound++;
                    return 1;
                }
            }

        }
        contNotFound++;
        return 0;
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
            System.out.println("OK: VirtualSensors '" + name + "' salvo em .dat!");
            return true;
        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println("ERROR: Falha ao salvar .dat de " + name);
            return false;
        }

    }

    public boolean salvarCsv(String name, int in, int fim, Mapping map) {

        //Delimiter used in CSV file
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        //CSV file header
        String FILE_HEADER = "From, To, Tempo, Qtde, Kind, Lat, Lon";

        FileWriter fileWriter = null;

        if (in < 0) {
            in = 0;
        }
        if (fim > discretTemporal) {
            fim = discretTemporal;
        }

        try {

            int t;
            fileWriter = new FileWriter(name + ".csv");
            fileWriter.append(FILE_HEADER);
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (t = in; t < fim; t++) { //laço temporal  
                //laço de Sensor
                for (int s = 0; s < getToNodCod().size(); s++) {

                    fileWriter.append(getFromNodCod().get(s));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(getToNodCod().get(s));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t * minInterv));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(contAresta[s][t]));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(arestaKind.get(s));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf((map.getNode_lat()[fromNodI.get(s)] + map.getNode_lat()[toNodI.get(s)]) / 2));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf((map.getNode_lon()[fromNodI.get(s)] + map.getNode_lon()[toNodI.get(s)]) / 2));
                    fileWriter.append(NEW_LINE_SEPARATOR);

                }//fim de laço de sensor

                //     fileWriter.append(NEW_LINE_SEPARATOR);       //     fileWriter.append(NEW_LINE_SEPARATOR);            
            } //fim laço de tempo

            System.out.println("OK: CSV VT '" + name + "' salvo!");

        } catch (IOException e) {
            System.out.println("ERROR: CsvFileWriter de '" + name + ".");
            // e.printStackTrace();
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

    /**
     * @return the fromNodI
     */
    public ArrayList<Integer> getFromNodIndex() {

        if (fromNodI.size() < 1) {
            System.out.println("ERROR: fromNodI em VirtualSensors está vazio!");
        }
        return fromNodI;
    }

    /**
     * @return the toNodI
     */
    public ArrayList<Integer> getToNodIndex() {
        if (toNodI.size() < 1) {
            System.out.println("ERROR: toNodI em VirtualSensors está vazio!");
        }

        return toNodI;
    }

    /**
     * @return the fromNod
     */
    public ArrayList<String> getFromNodCod() {

        if (fromNod.size() < 1) {
            System.out.println("ERROR: fromNod em VirtualSensors está vazio!");
        }
        return fromNod;
    }

    /**
     * @return the toNod
     */
    public ArrayList<String> getToNodCod() {
        if (toNod.size() < 1) {
            System.out.println("ERROR: toNod em VirtualSensors está vazio!");
        }
        return toNod;
    }

    public int getContArestaTotalAresta(Integer from, Integer to) {

        for (int x = 0; x < fromNodI.size(); x++) {
            if (Objects.equals(fromNodI.get(x), from)) {
                if (Objects.equals(toNodI.get(x), to)) {
                    return this.getContArestaTotalAresta(x);
                }
            }
        }

        System.out.println("ERROR: Não encontrou aresta " + from + " para " + to + "(procurou em " + fromNodI.size() + " arestas)");
        return -1;
    }

    public double variancia(int[] x) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int z = 0; z < x.length; z++) {
            stats.addValue(x[z]);
        }
        return stats.getVariance();
    }

    public double varianciaS(int[] x) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int z = 0; z < x.length; z++) {
            stats.addValue(x[z]);
        }
        return stats.getVariance();
    }

    public int findIndiceAresta(int indFrom, int indTo) {

        for (int a = 0; a < fromNodI.size(); a++) {
            if (fromNodI.get(a) == indFrom && toNodI.get(a) == indTo) {
                return a;
            }
        }
        return -1;
    }

}
