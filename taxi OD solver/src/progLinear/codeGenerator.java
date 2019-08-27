/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.

        network net = new network();
        codeGenerator cod = new codeGenerator();
        cod.printCodigo(net);

 */
package progLinear;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Luciano
 */
public class codeGenerator {
    
     
    String tipoNSLP;
    
    network net;
    String tipo; //min, max, intv
    
    public void setTipoNSLP(String t){
        tipoNSLP = t;
    }
    
    
    public void printCodigo(network nt, String t){
    tipo = t;
    net = nt;
    String code ="#GUSEK CODE BELOW:\n";
        
    //declara variaveis
    //code = code + "#Declaracao de variaveis:\n#Binarias de sensores ativos:\n"; //RETIRAR. Todos os sensores serão ativos agora
    //code = code + declBinariasSensor();
    //code = code + "\nvar desvioMax = 12;";
    code = code + "\n#Veiculos por rota:\n";
    code = code + declQuantidadesRota();
    code = code + "\n#Veiculos em sensor/aresta:\n";
    code = code + declVeiculoSensor();
    if(t.equals("intv")){
    code = code + "\n#Folgas Sensores:\n";
    code = code + declFolgaSensor();
        
    }
        
    //função objetivo
    code = code + "\n\n#Funcao objetivo:\n";  //MODIFICAR
    //code = code + funcaoFitnessMinTotalSensores();
    code = code + funcaoFitness();
    
    //declara restrições
    code = code + "\n\n#Conjunto de restricoes:";
    //code = code + "\n#Min e Max de sensores ativos (Total: 154):";         //RETIRAR
   // code = code + restrQuantSensor();
    //code = code + "\n#Declaracao de onde estao os sensores:";         //Estao em todos os lugares.
    //code = code + "\n#Toda rota deve estar coberta por pelo menos 2 sensores:";  //RETIRAR
   // code = code + coberturaODSensor();
    code = code + "\n#Cada sensor precisa ter medição >= que o real:";
    code = code + noMinimoIgualarSensores();
   // code = code + "\n#Cada sensor  precisa ter medição <= que o real+desvio:";
   // code = code + desvioMaximoSensores();
    code = code + "\n#Contabilizando veículos por link a partir das rotas que passam pelo link:"; //MODIFICAR
    code = code + restrCalcularQuantLink();
    //code = code + "\n#Calibragem para evitar OD pairs muito grandes:"; //??????
   // code = code + calibragemODPairs();
    
    //fim do código
    code = code + "\n\nsolve;";
    code = code + "\nprintf \"\\n\\n#QUALIDADE DE FLUXO NOS SENSORES:\\n\";";
    code = code + printFRelat();
    code = code + "\n\nend;\n\n";
    
    //System.out.println(code);
    criaDiretorio("GUSEK");
    salvarTxt("GUSEK\\Codigo GUSEK "+tipo+" p"+net.getTempoPriori()+"t"+net.getTempoProblema()+" "+tipoNSLP+", "+net.getNumeroSensores()+"sens.mod",code);
    System.out.println("END: Terminou de gerar codigo para Codigo GUSEK p"+net.getTempoPriori()+"t"+net.getTempoProblema()+" "+tipoNSLP+".mod");
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
    
    public String declBinariasSensor(){
    String s = "";
    int [] aresta1 = net.getAresta1();
    int [] aresta2 = net.getAresta2();
    
    for(int a=0;a<aresta1.length;a++){
        s = s+ "var b"+aresta1[a]+"sen"+aresta2[a]+" binary; ";
        s = s+ "var b"+aresta2[a]+"sen"+aresta1[a]+" binary; ";
    }
    return s;
    }
    
    public String declQuantidadesRota(){
    String s = "";
    int [] aresta1 = net.getParOD1();
    int [] aresta2 = net.getParOD2();
    
    for(int a=0;a<aresta1.length;a++){
        s = s+ "var OD"+aresta1[a]+"to"+aresta2[a]+" >=0; ";
      
        }
    return s;
    }
    
    /*public String restrQuantidadesRota(){
    String s = "";
    int [] aresta1 = net.getParOD1();
    int [] aresta2 = net.getParOD2();
   
    for(int a=0;a<aresta1.length;a++){
        s = s+ "\nsubj to r"+aresta1[a]+"to"+aresta2[a]+": "+aresta1[a]+"to"+aresta2[a]+" >= 0; ";
      
        }
    return s;
    }*/
    
    public String restrQuantSensor(){
    String s = ""; String t = "";
    int [] aresta1 = net.getAresta1();
    int [] aresta2 = net.getAresta2();
    
    for(int a=0;a<aresta1.length;a++){
        s = s+ "b"+aresta1[a]+"sen"+aresta2[a]+"+";
        s = s+" b"+aresta2[a]+"sen"+aresta1[a]+"+";
    }
    
    s = "("+s.substring(0, s.length()-1)+")";
    
    t = "\nsubj to minSen: "+ s +" >= 50; \nsubj to maxSen: "+ s +" <= 130;";
    
    return t;
    }

    private String declFolgaSensor() {
    String s = "";
    int [] aresta1 = net.getAresta1();
    int [] aresta2 = net.getAresta2();
    int [] cods = net.getCodigosArestas();
    int cont=0;
    
    for(int a=0;a<aresta1.length;a++){
        if(!net.STRINGsomaRotasPassamPorSensor2(cods[a]).equals("0")){
            s = s+ "var fMenos"+aresta1[a]+"to"+aresta2[a]+" >=0; var fMais"+aresta1[a]+"to"+aresta2[a]+" >=0; ";
            cont++;
        }
        
       // s = s+ "var C"+aresta2[a]+"to"+aresta1[a]+" >=0; ";
    }
        System.out.println("#"+cont+"/"+aresta1.length+" arestas ok");
    return "#"+cont+"/"+aresta1.length+" arestas ok\n"+s.substring(0, s.length()-1);
    }
    
    
    private String declVeiculoSensor() {
       String s = "";
    int [] aresta1 = net.getAresta1();
    int [] aresta2 = net.getAresta2();
    int [] cods = net.getCodigosArestas();
    
    for(int a=0;a<aresta1.length;a++){
        if(!net.STRINGsomaRotasPassamPorSensor2(cods[a]).equals("0"))
            s = s+ "var C"+aresta1[a]+"to"+aresta2[a]+" >=0; ";
       // s = s+ "var C"+aresta2[a]+"to"+aresta1[a]+" >=0; ";
    }
    return s.substring(0, s.length()-1);
    }
    
    public String restrCalcularQuantLink(){
    String s = ""; 
    int [] aresta1 = net.getAresta1();
    int [] aresta2 = net.getAresta2();
    int [] codAresta = net.getCodigosArestas();
    String sx;
    
    for(int a=0;a<aresta1.length;a++){
        sx = net.STRINGsomaRotasPassamPorSensor2(codAresta[a]);
        if(sx.length()>3){
        s = s+ "\nsubj to Cal"+aresta1[a]+"to"+aresta2[a]+": "
                + "C"+aresta1[a]+"to"+aresta2[a]+" = "+sx+";";
        }
  
    }
    
    return s;
    }

    private String coberturaODSensor() {
      
    String s = "";
    int [] a1 = net.getParOD1();
    int [] a2 = net.getParOD2();
    
    for(int a=0;a<a1.length;a++){
        s = s+  net.STRINGcoberturaDeRotaPorSensor(a1[a], a2[a]);
      
    }
    
    return s;
    }
    
    public String funcaoFitness(){
        
     //min, max, intv    
     String s;
    if(tipo.equals("min") || tipo.equals("intv") )    
        s = "minimize z: ";
    else if(tipo.equals("max"))
        s = "maximize z: ";
    else{
            System.out.println("ERROR: Tipo invalido em codeGenerator.funcaoFitness");
            s = "";
    }
    //sensor a sensor:   b17sen18(900 - C17to18)
    int [] a1 = net.getAresta1();
    int [] a2 = net.getAresta2();
    int [] codArestas = net.getCodigosArestas();
    
    for(int a=0;a<a1.length;a++)
        if(!net.STRINGsomaRotasPassamPorSensor2(codArestas[a]).equals("0"))
            if(tipo.equals("min") || tipo.equals("max") )
                s = s + "(C"+a1[a]+"to"+a2[a]+" - "+net.getFluxoPorAresta(codArestas[a])+") + ";
             else
                s = s + "(fMais"+a1[a]+"to"+a2[a]+" + fMenos"+a1[a]+"to"+a2[a]+") + ";
              //  + " "b"+a1[a]+"sen"+a2[a]+"*" (C"+a1[a]+"to"+a2[a]+" - "+net.INTSomaRotasPassamPorSensor(a1[a], a2[a])+")*b"+a1[a]+"sen"+a2[a]+" + ";
                     
    return s.substring(0, s.length()-2)+";";
    }
    
    public String funcaoFitnessMinTotalSensores(){
    String s = "minimize z: ";
    //sensor a sensor:   b17sen18(900 - C17to18)
    int [] a1 = net.getAresta1();
    int [] a2 = net.getAresta2();
    int [] cs = net.getCodigosArestas();
    
    for(int a=0;a<a1.length;a++)
        s = s + " C"+a1[a]+"to"+a2[a]+" - (b"+a1[a]+"sen"+a2[a]+"*"+net.getFluxoPorAresta(cs[a])+") +";
    
    return s.substring(0, s.length()-2)+";";
    }
    
    
    public String noMinimoIgualarSensores(){
    String s = "";
    //sensor a sensor:   b17sen18(900 - C17to18)
    int [] a1 = net.getAresta1();
    int [] a2 = net.getAresta2();
    int [] cArs = net.getCodigosArestas();
   
    for(int a=0;a<a1.length;a++){
        if(!net.STRINGsomaRotasPassamPorSensor2(cArs[a]).equals("0")){
        if(tipo.equals("min"))
            s = s + "\nsubj to min"+a1[a]+"sen"+a2[a]+": "
                + " C"+a1[a]+"to"+a2[a]+" >= "+net.getFluxoPorAresta(cArs[a])+";";       
        else if(tipo.equals("max"))
            s = s + "\nsubj to min"+a1[a]+"sen"+a2[a]+": "
                + " C"+a1[a]+"to"+a2[a]+" <= "+net.getFluxoPorAresta(cArs[a])+";";       
        else if(tipo.equals("intv")){
            
            s = s + "\nsubj to min"+a1[a]+"sen"+a2[a]+": "
                + " C"+a1[a]+"to"+a2[a]+" + fMais"+a1[a]+"to"+a2[a]+" - fMenos"+a1[a]+"to"+a2[a]+"  = "+net.getFluxoPorAresta(cArs[a])+";";       
        
        }else
                System.out.println("ERROR: Sem opcao de tipo valida em codeGenerator.noMinimoIgualarSensores = "+tipo);
            
        }
    }
    
    
    return s;
    }
    
    
    public String desvioMaximoSensores(){
    String s = "";
    //sensor a sensor:   b17sen18(900 - C17to18)
    int [] a1 = net.getAresta1();
    int [] a2 = net.getAresta2();
    int [] cArs = net.getCodigosArestas();
   
    for(int a=0;a<a1.length;a++){
        if(!net.STRINGsomaRotasPassamPorSensor2(cArs[a]).equals("0"))
        s = s + "\nsubj to max"+a1[a]+"sen"+a2[a]+": "
                + " C"+a1[a]+"to"+a2[a]+" <= ("+net.getFluxoPorAresta(cArs[a])+" +desvioMax);";       
    }
    
    
    return s;
    }
    
    
    

    private String calibragemODPairs() {
        String s = "";
        int [] a1 = net.getParOD1();
    int [] a2 = net.getParOD2();
    
    for(int a=0;a<a1.length;a++){
        s = s+  "\nsubj to calib"+a1[a]+"to"+a2[a]+": OD"+a1[a]+"to"+a2[a]+" <= 30;";
      
    }
    
    return s;
        
        
    }

    private String printFRelat() {
       String s = "";
       //printf "! %3d | %3d | %3d  | %3d  |  \n", 1, 
       //(1*m1s1t1+2*m1s1t2+3*m1s1t3), m1i1, m1f1;
       int [] a1 = net.getAresta1();
       int [] a2 = net.getAresta2();
       int [] cs = net.getCodigosArestas();
    
       for(int a=0;a<a1.length;a++)
           if(net.getFluxoPorAresta(cs[a])!=0  &  (!net.STRINGsomaRotasPassamPorSensor2(cs[a]).equals("0")))
           s = s + "\nprintf \"!C"+a1[a]+"to"+a2[a]+": %3d / "
                   + ""+net.getFluxoPorAresta(cs[a])+" = %10.2f \\n\", C"+a1[a]+"to"+a2[a]+", C"+a1[a]+"to"+a2[a]+"/"+net.getFluxoPorAresta(cs[a])+";";
    //   else
//               s = s + "\nprintf \"!C"+a1[a]+"to"+a2[a]+": 0 \\n\";";
       
         s = s + "\nprintf \"\\n\\n#QUALIDADE DE FLUXO O-D:\\n\";";
         
       
         
         
       a1 = net.getParOD1();
       a2 = net.getParOD2();  
         
       for(int a=0;a<a1.length;a++)
           if(net.getQuantidadeCarros(a)>0)
           s = s + "\nprintf \"!OD"+a1[a]+"to"+a2[a]+": %3d / "
                   + ""+net.getQuantidadeCarros(a)+" = %10.2f \\n\", OD"+a1[a]+"to"+a2[a]+", OD"+a1[a]+"to"+a2[a]+"/"+net.getQuantidadeCarros(a)+";";
         else
               s = s + "\nprintf \"!OD"+a1[a]+"to"+a2[a]+": %3d / "
                   + ""+net.getQuantidadeCarros(a)+" = %10.2f \\n\", OD"+a1[a]+"to"+a2[a]+", OD"+a1[a]+"to"+a2[a]+"/"+0.00001+";";
         
       s = s + "\nprintf \"\\n\\n#Resultado a ser digerido pelo sistema:\\n\\n\";";
       s = s + net.codPrintTodosODPar(net.getTempoPriori() + " " + net.getTempoProblema() +" " + cs.length + " "+ tipoNSLP);
       
       return s;
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
    
    
}
