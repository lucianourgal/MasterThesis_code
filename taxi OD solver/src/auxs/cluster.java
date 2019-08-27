/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import taxi.od.solver.Mapping;
import taxi.od.solver.VirtualSensors;

/**
 * @author luciano
 */
public class cluster implements Serializable {

    private ArrayList<bloco> blocos;

    private ArrayList<Integer> nosBordaFrom;
    private ArrayList<Integer> nosBordaTo;

    private ArrayList<Double> nosBordaLat;
    private ArrayList<Double> nosBordaLon;

    private boolean[] keepNode;
    private int[] trafegoPorAresta;

    final DecimalFormat df2 = new DecimalFormat(".##");

    public int limitarNumArestasDeSaida(VirtualSensors vt, Double porc) {

        int contagem = 0;

        cadastrarTrafegoDasArestas(vt);

        int somaTraf = 0;
        int[] ordem;
        ordem = new int[nosBordaFrom.size()];

        for (int x = 0; x < nosBordaFrom.size(); x++) {
            somaTraf += trafegoPorAresta[x];
            ordem[x] = -1;
        }

        int ocup = 0;

        for (int a = 0; a < nosBordaFrom.size(); a++) //aresta em aresta
        {
            for (int n2 = 0; n2 < nosBordaFrom.size(); n2++) { // de posição de mov em posição, até que 

                if (ordem[n2] == -1) { // encontre uma posição vaga,
                    ordem[n2] = a;//fromNodI.get(a);
                    //mov2[n2]= toNodI.get(a);
                    n2 = nosBordaFrom.size();
                    ocup++;
                }
                //     System.out.println("n2="+n2+";  a="+a + "; fromNodSize="+fromNod.size());
                //   System.out.println("mov[n2]="+mov[n2]+";  a="+a + "; fromNodSize="+fromNod.size());

                if (n2 != nosBordaFrom.size()/*  && !jaUtilizado*/) {
                    if (trafegoPorAresta[ordem[n2]] < trafegoPorAresta[a]) { // OU, encontre aresta com total maior que o da aresta na posição.

                        //verifica se já não existe no vetor    
                        //passa todos para a direita
                        for (int x = ocup - 2; x >= n2; x--) {
                            if ((x + 1) < nosBordaFrom.size()) {
                                ordem[x + 1] = ordem[x];
                            }

                        }

                        ordem[n2] = a;//fromNodI.get(a);  //index de aresta
                        n2 = nosBordaFrom.size() + 1;

                        if (ocup < nosBordaFrom.size()) {
                            ocup++;
                        }
                    }
                }
            }
        }

        ////// Encontra arestas que devem permanecer pós redução
        double limite = porc * ((double) somaTraf);
        double acum = 0;

        System.out.println(nosBordaFrom.size() + " bordas, soma " + somaTraf + ". Limite = " + limite);

        if (keepNode == null) {
            keepNode = new boolean[nosBordaFrom.size()];

            for (int b = 0; b < nosBordaFrom.size(); b++) {
                keepNode[b] = false;
            }

        }

        for (int a = 0; a < nosBordaFrom.size(); a++) { //aresta em aresta

         if(ordem[a]!=-1){
            
            acum = acum + trafegoPorAresta[ordem[a]];
            keepNode[ordem[a]] = true;
            contagem++;

            if (acum >= limite) {
                a = nosBordaFrom.size();
            }

        }
            
            
        }

        contagem = nosBordaFrom.size() - contagem;

        clearNotKeepBordas();

        return contagem;

    }

    private void cadastrarTrafegoDasArestas(VirtualSensors vt) {

        trafegoPorAresta = new int[nosBordaFrom.size()];
        int c;
        int er=0;
        
        for (int x = 0; x < nosBordaFrom.size(); x++) {
            c = vt.getContArestaTotalAresta(nosBordaFrom.get(x), nosBordaTo.get(x));
            
            if(c>=0)
                trafegoPorAresta[x] = c;
            else 
                er++;
        }

        if(er>0)
        System.out.println("ALERT: "+er+"/"+nosBordaFrom.size()+" falhas em obter getContArestaTotalAresta");
        
    }

    public cluster() {

        blocos = new ArrayList<>();

    }

    public int getOrigensCont(int t) {
        int c = 0;

        for (int b = 0; b < blocos.size(); b++) {
            c += blocos.get(b).getOrigensCont(t);
        }

        return c;
    }

    public int getDestinosCont(int t) {
        int c = 0;

        for (int b = 0; b < blocos.size(); b++) {
            c += blocos.get(b).getDestinosCont(t);
        }

        return c;
    }

    public boolean pontoPertenceAoCluster(double lat, double lon) {

        for (int b = 0; b < blocos.size(); b++) {
            if (blocos.get(b).pontoPertenceAoBloco(lat, lon)) {
                return true;
            }
        }

        return false;
    }

    public boolean temBloco(int ilat, int ilon) {

        for (int b = 0; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getIndexLat() == ilat && getBlocos().get(b).getIndexLon() == ilon) {
                return true;
            }
        }

        return false;
    }

    public boolean podeAdicionarBloco(bloco bl) {

        //precisa ser vizinho lat
        /*if(bl.getIndexLat() == (this.getIndexLatSup()+1) || bl.getIndexLat() == (this.getIndexLatInf()-1) || 
                ( bl.getIndexLat() < (this.getIndexLatSup()+1) && bl.getIndexLat() > (this.getIndexLatInf()-1) )  ){
            if(bl.getIndexLon() == (this.getIndexLonSup()+1) || bl.getIndexLon() == (this.getIndexLonInf()-1) )
                return true;
        }*/
        //se for vizinho em qualquer direção, e cluster só tem um bloco
        if (getBlocos().size() == 1) {

            /*if(bl.getIndexLat() == (this.getIndexLatSup()+1) || bl.getIndexLat() == (this.getIndexLatInf()-1) ||
                 bl.getIndexLon() == (this.getIndexLonSup()+1) || bl.getIndexLon() == (this.getIndexLonInf()-1)   )*/
            if (temBloco(bl.getIndexLat() - 1, bl.getIndexLon()) || temBloco(bl.getIndexLat() + 1, bl.getIndexLon())
                    || temBloco(bl.getIndexLat(), bl.getIndexLon() - 1) || temBloco(bl.getIndexLat(), bl.getIndexLon() + 1)) {
                return true;
            }

        } else { //se tiver dois ou mais blocos, precisa de vizinho do lado, e em alguma transversal

            //acima
            if ((temBloco(bl.getIndexLat() - 1, bl.getIndexLon()))
                    && (temBloco(bl.getIndexLat() - 1, bl.getIndexLon() + 1) || temBloco(bl.getIndexLat() - 1, bl.getIndexLon() - 1))) {
                return true;
            }
            //abaixo
            if (temBloco(bl.getIndexLat() + 1, bl.getIndexLon())
                    && (temBloco(bl.getIndexLat() + 1, bl.getIndexLon() + 1) || temBloco(bl.getIndexLat() + 1, bl.getIndexLon() - 1))) {
                return true;
            }
            //esquerda
            if (temBloco(bl.getIndexLat(), bl.getIndexLon() + 1)
                    && (temBloco(bl.getIndexLat() + 1, bl.getIndexLon() + 1) || temBloco(bl.getIndexLat() - 1, bl.getIndexLon() + 1))) {
                return true;
            }
            //direita
            if (temBloco(bl.getIndexLat(), bl.getIndexLon() - 1)
                    && (temBloco(bl.getIndexLat() + 1, bl.getIndexLon() - 1) || temBloco(bl.getIndexLat() - 1, bl.getIndexLon() - 1))) {
                return true;
            }

        }

        return false;
    }

    public double getMediaDensidade(int t) {
        double med = 0;
        for (int b = 0; b < getBlocos().size(); b++) {
            med += getBlocos().get(b).getOrigensCont(t) + getBlocos().get(b).getDestinosCont(t);
        }

        return (med / getBlocos().size());
    }

    public double custoDeAdicionar(int t, bloco bl) {

        if (!podeAdicionarBloco(bl)) {
            return 99999;
        }

        double med = getMediaDensidade(t);
        int x = bl.getDestinosCont(t) + bl.getOrigensCont(t);

        if (med > x) {
            return med - x + getBlocos().size() * 0.0001;
        } else {
            return x - med + getBlocos().size() * 0.0001;
        }
    }

    public void addBloco(bloco bl) {
        getBlocos().add(bl);
    }

    public int getBlocosSize() {
        return getBlocos().size();
    }

    
    public String getStrCentroide(){
        if(blocos.isEmpty())
            return "";
        return ((getLatSup()+getLatInf())/2)+", "+ ((getLonSup()+getLonInf())/2) + " - Lat "+getLatInf()+" at "+getLatSup()+". Lon "+getLonInf()+" at "+getLonSup();
    
    }
    
    public double getCentroLat(){
    return ((getLatSup()+getLatInf())/2);
    }
    public double getCentroLon(){
    return ((getLonSup()+getLonInf())/2);
    }
    
    public double getLatSup() {
        if(blocos.isEmpty())
            return -1;
        
        double n = getBlocos().get(0).getLatSup();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getLatSup() > n) {
                n = getBlocos().get(b).getLatSup();
            }
        }

        return n;
    }

    public double getLatInf() {
        if(blocos.isEmpty())
            return -1;
        
        double n = getBlocos().get(0).getLatInf();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getLatInf() < n) {
                n = getBlocos().get(b).getLatInf();
            }
        }

        return n;
    }

    //lon
    public double getLonSup() {
        if(blocos.isEmpty())
            return -1;
        double n = getBlocos().get(0).getLonSup();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getLonSup() > n) {
                n = getBlocos().get(b).getLonSup();
            }
        }

        return n;
    }

    public double getLonInf() {
        if(blocos.isEmpty())
            return -1;
        double n = getBlocos().get(0).getLonInf();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getLonInf() < n) {
                n = getBlocos().get(b).getLonInf();
            }
        }

        return n;
    }

    //index lat
    public int getIndexLatSup() {
        if(blocos.isEmpty())
            return -1;
        
        int n = getBlocos().get(0).getIndexLat();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getIndexLat() > n) {
                n = getBlocos().get(b).getIndexLat();
            }
        }

        return n;
    }

    public int getIndexLatInf() {
        if(blocos.isEmpty())
            return -1;
        int n = getBlocos().get(0).getIndexLat();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getIndexLat() < n) {
                n = getBlocos().get(b).getIndexLat();
            }
        }

        return n;
    }

    //index lon
    public int getIndexLonSup() {
        if(blocos.isEmpty())
            return -1;
        int n = getBlocos().get(0).getIndexLon();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getIndexLon() > n) {
                n = getBlocos().get(b).getIndexLon();
            }
        }

        return n;
    }

    public int getIndexLonInf() {
        if(blocos.isEmpty())
            return -1;
        int n = getBlocos().get(0).getIndexLon();

        for (int b = 1; b < getBlocos().size(); b++) {
            if (getBlocos().get(b).getIndexLon() < n) {
                n = getBlocos().get(b).getIndexLon();
            }
        }

        return n;
    }

    /**
     * @return the blocos
     */
    public ArrayList<bloco> getBlocos() {
        return blocos;
    }

    public int getNumeroNosBorda() {
        return nosBordaFrom.size();
    }

    public int encontrarNosBordaREDUX(Mapping mapa) {

        nosBordaFrom = new ArrayList<>();
        nosBordaTo = new ArrayList<>();
        nosBordaLat = new ArrayList<>();
        nosBordaLon = new ArrayList<>();
        boolean adicionar;
        int contN = 0;

        for (int a = 0; a < mapa.getContNodes(); a++) { //de nó em nó

            adicionar = false;
            //  System.out.println("Testando "+latSup+" "+latInf+" lon "+lonSup+" "+lonInf +" E "+node_lat[a]+"/"+node_lon[a]);

            if (pontoPertenceAoCluster(mapa.getNode_lat()[a], mapa.getNode_lon()[a]) && !nosBordaFrom.contains(a)) { //se nó pertence ao cluster, e ainda não foi adicionado para borda

                //verifica se o nó tem alguma conexão com um nó não pertencente ao cluster
                ArrayList<Integer> vizinhos = mapa.getVizinhosIndexDoNode(a, false);
                int viz = -1;

                for (int v = 0; v < mapa.getContVizinhosNode(a, false); v++) //if(this.nodeLocalizado(mapa.getNode_id()[vizinhos[v]], node_id, node_lat, node_lon, cont, latSup, latInf, lonSup, lonInf, vert, hori))
                {
                    if (!this.pontoPertenceAoCluster(mapa.getNode_lat()[vizinhos.get(v)], mapa.getNode_lon()[vizinhos.get(v)])) { //se um dos vizinhos está fora do cluster
                        adicionar = true; //achou um vizinho que está fora do cluster
                        viz = v;
                        v = mapa.getContVizinhosNode(a, false); //não precisa testar mais. Já encontrou ponto fora
                    }
                }

                if (adicionar) {
                    nosBordaFrom.add(a);
                    nosBordaTo.add(vizinhos.get(viz));

                    nosBordaLat.add(  (mapa.getNode_lat()[a]+mapa.getNode_lat()[vizinhos.get(viz)])/2 );
                    nosBordaLon.add(  (mapa.getNode_lon()[a]+mapa.getNode_lon()[vizinhos.get(viz)])/2 );
                    contN++;
                }
            }

        }

        return contN;

    }

    /**
     * 
     * @deprecated
     * @param bordaM
     * @param mapa 
     */
    private void encontrarNosBorda(double bordaM, Mapping mapa) {

        nosBordaFrom = new ArrayList<>();
        nosBordaTo = new ArrayList<>();
        nosBordaLat = new ArrayList<>();
        nosBordaLon = new ArrayList<>();

        //encontrar bordas
        for (int b = 0; b < blocos.size(); b++) {
            //encontrar pontos perto das bordas
            if (!temBloco(blocos.get(b).getIndexLat() + 1, blocos.get(b).getIndexLon())) { //acima livre
                adicionarNodesDaRegiao(blocos.get(b).getLatSup(), blocos.get(b).getLatSup() - (bordaM * 0.0000089), blocos.get(b).getLonSup(), blocos.get(b).getLonInf(), mapa, 1, 0);
            }
            if (!temBloco(blocos.get(b).getIndexLat() - 1, blocos.get(b).getIndexLon())) { //abaixo livre
                adicionarNodesDaRegiao(blocos.get(b).getLatInf() + (bordaM * 0.0000089), blocos.get(b).getLatInf(), blocos.get(b).getLonSup(), blocos.get(b).getLonInf(), mapa, -1, 0);
            }
            if (!temBloco(blocos.get(b).getIndexLat(), blocos.get(b).getIndexLon() + 1)) { //livre a direita
                adicionarNodesDaRegiao(blocos.get(b).getLatSup(), blocos.get(b).getLatInf(), blocos.get(b).getLonSup(), blocos.get(b).getLonSup() - (bordaM * 0.0000089), mapa, 0, 1);
            }
            if (!temBloco(blocos.get(b).getIndexLat(), blocos.get(b).getIndexLon() - 1)) { //livre a esquerda
                adicionarNodesDaRegiao(blocos.get(b).getLatSup(), blocos.get(b).getLatInf(), blocos.get(b).getLonInf() + (bordaM * 0.0000089), blocos.get(b).getLonInf(), mapa, 0, -1);
            }

        }

    }

    private int adicionarNodesDaRegiao(double latSup, double latInf, double lonSup, double lonInf, Mapping mapa, int vert, int hori) {
        int contN = 0;
        String[] node_id = mapa.getNode_id();
        Double[] node_lat = mapa.getNode_lat();
        Double[] node_lon = mapa.getNode_lon();
        ArrayList<String>[] way_nodes = mapa.getWay_nodes();
        int cont = mapa.getContNodes();
        int contW = mapa.getWayCount();
        boolean adicionar;

        for (int a = 0; a < cont; a++) { //de nó em nó

            adicionar = false;
            //  System.out.println("Testando "+latSup+" "+latInf+" lon "+lonSup+" "+lonInf +" E "+node_lat[a]+"/"+node_lon[a]);
            /*if (((node_lat[a] <= latSup && node_lat[a] >= latInf) || (node_lat[a] >= latSup && node_lat[a] <= latInf))
                    && ((node_lon[a] <= lonSup && node_lon[a] >= lonInf) || (node_lon[a] >= lonSup && node_lon[a] <= lonInf))) {*/ //nó A está na borda

            if (this.pontoPertenceAoCluster(node_lat[a], node_lon[a]) && !nosBordaFrom.contains(a)) {

                //encontra ways dos quais faz parte
                /*   for (int w = 0; w < contW; w++) {
                    for (int c = 0; c < way_nodes[w].size(); c++) {
                        if (way_nodes[w].get(c).equals(node_id[a])) { //node está neste way

                            //passa novamente pelos nós do way
                            for (int n = 0; n < way_nodes[w].size(); n++) {

                                adicionar = this.nodeLocalizado(way_nodes[w].get(n), node_id, node_lat, node_lon, cont, latSup, latInf, lonSup, lonInf, vert, hori);

                                if (adicionar) {
                                    n = way_nodes[w].size();
                                }
                            }

                            if (adicionar) { //já adicionou nó. Pode continuar procurando outros nós que se adequem
                                c = way_nodes[w].size();
                                w = contW;
                            }
                        }
                    }
                }*/
                //verifica se o nó tem alguma conexão com um nó não pertencente ao cluster
                ArrayList<Integer> vizinhos = mapa.getVizinhosIndexDoNode(a, false);
                int viz = -1;

                for (int v = 0; v < mapa.getContVizinhosNode(a, false); v++) //if(this.nodeLocalizado(mapa.getNode_id()[vizinhos[v]], node_id, node_lat, node_lon, cont, latSup, latInf, lonSup, lonInf, vert, hori))
                {
                    if (!this.pontoPertenceAoCluster(node_lat[vizinhos.get(v)], node_lon[vizinhos.get(v)])) {
                        adicionar = true; //achou um vizinho que está fora do cluster
                        viz = v;
                        v = mapa.getContVizinhosNode(a, false); //não precisa testar mais
                    }
                }

                if (adicionar) {
                    nosBordaFrom.add(a);
                    nosBordaTo.add(vizinhos.get(viz));

                    getNosBordaLat().add(node_lat[a]);
                    getNosBordaLon().add(node_lon[a]);
                    contN++;
                }
            }

        }

        return contN;
    }

    public boolean nodeLocalizado(String node, String[] node_id, Double[] node_lat, Double[] node_lon, int contN, double latSup, double latInf, double lonSup, double lonInf, int vert, int hori) {

        for (int a = 0; a < contN; a++) //de nó em nó
        {
            if (node.equals(node_id[a])) { //encontrou nó

                if (vert == 1) { // se for acima

                    return ((node_lat[a] >= latSup) && ((node_lon[a] <= lonSup && node_lon[a] >= lonInf) || (node_lon[a] >= lonSup && node_lon[a] <= lonInf))); //acima latSup e dentro dos limites de longitude

                } else if (vert == -1) { //se for abaixo

                    return ((node_lat[a] <= latInf) && ((node_lon[a] <= lonSup && node_lon[a] >= lonInf) || (node_lon[a] >= lonSup && node_lon[a] <= lonInf))); //abaixo e dentro dos limites de longitude

                } else if (hori == 1) { //se for a direita

                    return (((node_lat[a] <= latSup && node_lat[a] >= latInf) || (node_lat[a] >= latSup && node_lat[a] <= latInf))
                            && (node_lon[a] >= lonSup)); // dentro dos limites de latitude, acima lonSup

                } else { //se for a esquerda

                    return (((node_lat[a] <= latSup && node_lat[a] >= latInf) || (node_lat[a] >= latSup && node_lat[a] <= latInf))
                            && (node_lon[a] <= lonInf)); // dentro dos limites de latitude, abaixo lonInf

                }

            }
        }

        return false;
    }

    public String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));

    }

    /**
     * @return the nosBorda
     */
    public ArrayList<Integer> getNosBordaFrom() {
        if(nosBordaFrom!=null)
        return nosBordaFrom;
        else
            return new ArrayList<>();
    }

    public ArrayList<Integer> getNosBordaTo() {
        return nosBordaTo;
    }

    /**
     * @return the nosBordaLat
     */
    public ArrayList<Double> getNosBordaLat() {
        return nosBordaLat;
    }

    /**
     * @return the nosBordaLon
     */
    public ArrayList<Double> getNosBordaLon() {
        return nosBordaLon;
    }

    public int marcarNode(int n) {

        if (keepNode == null) {
            keepNode = new boolean[nosBordaFrom.size()];

            for (int b = 0; b < nosBordaFrom.size(); b++) {
                keepNode[b] = false;
            }

        }

        for (int b = 0; b < nosBordaFrom.size(); b++) {
            if (nosBordaFrom.get(b) == n) {
                keepNode[b] = true;
                return 1;
            }
        }

        return 0;
    }

    public void clearNotKeepBordas() {

        ArrayList<Integer> nosBordaX = new ArrayList<>();
        ArrayList<Integer> nosBordaY = new ArrayList<>();

        ArrayList<Double> nosBordaLatX = new ArrayList<>();
        ArrayList<Double> nosBordaLonX = new ArrayList<>();
        int[] traf = new int[nosBordaFrom.size()];

        for (int x = 0; x < nosBordaFrom.size(); x++) {
            if (keepNode[x]) {
                nosBordaX.add(nosBordaFrom.get(x));
                nosBordaY.add(nosBordaTo.get(x));

                nosBordaLatX.add(nosBordaLat.get(x));
                nosBordaLonX.add(nosBordaLon.get(x));
                traf[nosBordaX.size() - 1] = this.trafegoPorAresta[x];

            }
        }

        nosBordaFrom = nosBordaX;
        nosBordaTo = nosBordaY;

        nosBordaLat = nosBordaLatX;
        nosBordaLon = nosBordaLonX;

        trafegoPorAresta = traf;

    }

}
