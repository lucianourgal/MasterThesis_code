/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import jdistlib.Tukey;
import jdistlib.disttest.NormalityTest;
import jdistlib.util.Utilities;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.util.MathUtils;

/**
 *
 * @author lucia
 */
public class resultsAlgoritmos implements Serializable {

    private String filename = "resultsAlgs.dat";
    DecimalFormat df5 = new DecimalFormat("###.#####");
    DecimalFormat df3 = new DecimalFormat("###.###");

    private ArrayList<String> nomeDados;
    private ArrayList<String> tempos;
    private ArrayList<Integer> sensores;
    private ArrayList<double[][]> matrizOD;
    private ArrayList<Double[]> rmse;
    private ArrayList<Double[]> mae;
    private ArrayList<Double[]> tdd;
    private ArrayList<Double[]> re;
    private ArrayList<Double[]> geh;
    private ArrayList<Double[]> percGehAbaixo5;
    private ArrayList<Double[]> r2links;
    private ArrayList<Double[]> r2odm;
    private ArrayList<Double[]> fitness;

    private ArrayList<Double[]> gehInicial;
    private ArrayList<Double[]> rmseInicial;
    private ArrayList<Double[]> maeInicial;
    private ArrayList<Double[]> fitnessInicial;

    private int[] incompleteCont;

    public void joinWithFile(String f) {

        resultsAlgoritmos j = new resultsAlgoritmos();
        j.setFilename(f);
        j = j.recuperarArquivo();

        for (int x = 0; x < j.getNomeDados().size(); x++) {

            if (j.getContIncompleto(j.getNomeDados().get(x)) < j.getRmse().get(x).length) {
                //é incompleto
                this.addPartialResults(j.getNomeDados().get(x), j.getSensores().get(x), j.getRmse().get(x), j.getMae().get(x), j.getFitness().get(x), j.getGeh().get(x),
                        j.getRmseInicial().get(x), j.getMaeInicial().get(x), j.getFitnessInicial().get(x), j.getGehInicial().get(x), j.getFlatMatrizOD(x), j.getTempos().get(x),
                        j.getContIncompleto(j.getNomeDados().get(x)), j.getR2links().get(x), j.getR2odm().get(x), j.getPercGehAbaixo5().get(x), j.getTdd().get(x), j.getRe().get(x));
                //addPartialResults(String nome, Double[] rsme2, Double[] mae2, Double[] fitness2, Double[] rsmeIn, Double[] maeIn, Double[] fitnessIn, double[] mod, String tempo, int momento) {
            } else {
                //é completo      
                this.addResultados(j.getNomeDados().get(x), j.getSensores().get(x), j.getRmse().get(x), j.getMae().get(x), j.getFitness().get(x), j.getGeh().get(x),
                        j.getRmseInicial().get(x), j.getMaeInicial().get(x), j.getFitnessInicial().get(x), j.getGehInicial().get(x), j.getFlatMatrizOD(x), j.getTempos().get(x), j.getR2links().get(x), j.getR2odm().get(x), j.getPercGehAbaixo5().get(x), j.getTdd().get(x), j.getRe().get(x));

            }
        }

        this.salvarArquivo(true);

    }

    public int getContIncompleto(String name) {

        int ind = nomeDados.indexOf(name);
        if (ind > -1) {
            if (incompleteCont[ind] == rmse.get(ind).length) {
                return -1;
            } else {
                return incompleteCont[ind];
            }
        }
        return -2;
    }

    public int getIndexRunAnterior(String name) {

        return nomeDados.indexOf(name);

    }

    public double calcShapiroWilk(double[] dat) {

        try {
            Utilities.sort(dat);
            double s = NormalityTest.shapiro_wilk_statistic(dat);

            return s;
        } catch (Exception e) {
            return -1;
        }

    }

    public String calcPValue(ArrayList<Double[]> valores, int a, int b) {
        String s = "";

        TTest t = new TTest();

        if (valores.get(a) == null || valores.get(b) == null || valores.get(b).length < 1 || valores.get(a).length < 1) {
            return "null";
        }

        double[] g1 = new double[incompleteCont[a]];
        double[] g2 = new double[incompleteCont[b]];

        for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
            try {
                g1[a1] = valores.get(a)[a1];
            } catch (Exception e) {
                System.out.println("ERROR: ???? em " + nomeDados.get(a) + " (resultsAlgoritmos.calcPValue)");
                return "err";
            }
        }
        for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
            try {
                g2[a1] = valores.get(b)[a1];
            } catch (Exception e) {
                System.out.println("ERROR: ???? em " + nomeDados.get(a) + " (resultsAlgoritmos.calcPValue) ");//+ mae.get(a).toString());
                return "err";
            }
        }

        if (g1.length < 2 || g2.length < 2) {
            return "NA";
        }

        double r = t.tTest(g1, g2);

        if (r < 0.05) {
            return "< 0.05";
        } else {
            return String.valueOf(df3.format(r)).replace(",", ".");
        }

    }

    public String calcPValue(int a, int b, int medida) {
        //0 = mae, 1 = rmse, 2 = mae final vs inicial, 3 = rmse final vs inicial, 4 = geh, 5 = r2 links, 6 = r2 odm
        //System.out.println("calcPvalue("+a+";"+b+") "+incompleteCont[a]+"; "+incompleteCont[b] );
        //MannWhitneyUTest t = new MannWhitneyUTest();
        TTest t = new TTest();

        double[] g1 = new double[incompleteCont[a]];
        double[] g2 = new double[incompleteCont[b]];

        if (a > getMae().size() || b > getRmse().size()) {
            System.out.println("ERROR: Só existem " + getMae().size() + " MAEs e " + getRmse().size() + " RMSEs. Procurando " + a + " e " + b + " (resultsAlgoritmos)");
            return "NaN";
        }

        switch (medida) {
            case 0:
                for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
                    g1[a1] = getMae().get(a)[a1];
                }
                for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
                    g2[a1] = getMae().get(b)[a1];
                }
                break;
            case 1:
                for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
                    g1[a1] = getRmse().get(a)[a1];
                }
                for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
                    g2[a1] = getRmse().get(b)[a1];
                }
                break;
            case 2:
                for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
                    g1[a1] = getMae().get(a)[a1];
                }
                for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
                    g2[a1] = getMaeInicial().get(b)[a1];
                }
                break;
            case 3:
                for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
                    if (a == 5) {
                        System.out.println(a1);
                    }
                    g1[a1] = getRmse().get(a)[a1];
                }
                for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
                    g2[a1] = getRmseInicial().get(b)[a1];
                }
                break;
            case 4:
                for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
                    g1[a1] = getGeh().get(a)[a1];
                }
                for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
                    g2[a1] = getGeh().get(b)[a1];
                }
                break;
            case 5:
                for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
                    g1[a1] = this.r2links.get(a)[a1];
                }
                for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
                    g2[a1] = this.r2links.get(b)[a1];
                }
                break;
            case 6:
                for (int a1 = 0; a1 < incompleteCont[a]; a1++) {
                    g1[a1] = this.r2odm.get(a)[a1];
                }
                for (int a1 = 0; a1 < incompleteCont[b]; a1++) {
                    g2[a1] = this.r2odm.get(b)[a1];
                }
                break;
            default:
                break;
        }

        if (g1.length < 2 || g2.length < 2) {
            return "NA";
        }

        double r = t.tTest(g1, g2);

        if (r < 0.05) {
            return "< 0.05";
        } else {
            return String.valueOf(df3.format(r)).replace(",", ".");
        }

    }

    public int printDadosDisponiveis() {
        System.out.println("Dados disponíveis em resultsAlgoritmos.dat:");

        for (int a = 0; a < getNomeDados().size(); a++) {
            System.out.println(a + " - " + getNomeDados().get(a));
        }

        System.out.println("- - - - - - - - - - - -");
        return getNomeDados().size();
    }

    public void criarRelatorioReduzido(ArrayList<Integer> esc) {

        resultsAlgoritmos redux = new resultsAlgoritmos();

        String ext = "_";
        double[] mat;

        for (int a = 0; a < esc.size(); a++) {
            ext = ext + esc.get(a);
            mat = new double[getMatrizOD().get(a).length * getMatrizOD().get(a).length];

            for (int x = 0; x < getMatrizOD().get(a).length; x++) {
                for (int y = 0; y < getMatrizOD().get(a).length; y++) {
                    mat[x * getMatrizOD().get(a).length + y] = getMatrizOD().get(a)[x][y];
                }
            }

            redux.addResultados(getNomeDados().get(esc.get(a)), getSensores().get(esc.get(a)), getRmse().get(esc.get(a)), getMae().get(esc.get(a)), getFitness().get(esc.get(a)), this.getGeh().get(esc.get(a)),
                    getRmseInicial().get(esc.get(a)), getMaeInicial().get(esc.get(a)), getFitnessInicial().get(esc.get(a)), this.getGehInicial().get(esc.get(a)), mat, getTempos().get(esc.get(a)),
                    this.getR2links().get(esc.get(a)), this.getR2odm().get(esc.get(a)), this.getPercGehAbaixo5().get(esc.get(a)), this.getTdd().get(esc.get(a)), this.getRe().get(esc.get(a)));
            //(String nome, Double[] rsme2, Double[] mae2,Double[] fitness2 , Double[] rsmeIn, Double[] maeIn,Double[] fitnessIn , double[] mod, String tempo) {1
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("QST: Sobreescrever arquivo " + this.filename + "? (1 para sim, 0 para não)");
        int resp = scanner.nextInt();

        if (resp > 0) {
            redux.salvarArquivo(true);
            ext = "";
        }

        redux.salvarCSVDados(ext);
        redux.gerarBoxPlot(ext);

    }

    public String stats(Double[] dat) {
        //String s = "";
        DescriptiveStatistics d = new DescriptiveStatistics();
        double[] dat2 = new double[dat.length];
        int cont = 0;

        for (Double dat1 : dat) {
            if (dat1 != null) {
                d.addValue(dat1);
                cont++;
            }
        }

        for (int x = 0; x < cont; x++) {
            dat2[x] = dat[x];
        }

        return String.valueOf(df5.format(d.getMin())).replace(",", ".") + ", " + String.valueOf(df5.format(d.getMean())).replace(",", ".")
                + ", " + String.valueOf(df5.format(d.getPercentile(50))).replace(",", ".") + ", " + String.valueOf(df5.format(d.getMax())).replace(",", ".") + ", " + d.getStandardDeviation() + "," + this.calcShapiroWilk(dat2) + ", ";
    }

    public void salvarCSVDados(String ext) {
        String data = ",";
        String table = "\\begin{table}[htb]\n"
                + "\\centering\n"
                + "\\caption{Desempenho dos algoritmos}\n"
                + "\\label{t:resultadosAlgor}\n"
                + "\\begin{tabular}{|r|cccc|}\n"
                + "\\hline\n &";

        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + getNomeDados().get(i) + ",";
            table = table + " " + getNomeDados().get(i).split("_")[0] + " &";
        }
        table = table.substring(0, table.length() - 1) + "\\\\ \\hline";
        String table2 = table;

        data = data + "\nMAE,";
        table = table + "\nMAE &";
        for (int i = 0; i < getNomeDados().size(); i++) {
            //System.out.println("mae.get("+i+") "+nomeDados.get(i));
            data = data + menorDoArray(mae.get(i)) + ",";
            table = table + " " + df3.format(menorDoArray(mae.get(i))) + " &";
        }
        table = table.substring(0, table.length() - 1) + "\\\\";

        data = data + "\nRMSE,";
        table = table + "\nRMSE &";
        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + menorDoArray(rmse.get(i)) + ",";
            table = table + " " + df3.format(menorDoArray(rmse.get(i))) + " &";
        }
        table = table.substring(0, table.length() - 1) + "\\\\";

        data = data + "\nGEH,";
        table = table + "\nGEH &";
        table2 = table2 + "\nGEH &";
        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + menorDoArray(geh.get(i)) + ",";
            table = table + " " + df3.format(menorDoArray(geh.get(i))) + " &";
            table2 = table2 + " " + df3.format(menorDoArray(geh.get(i))) + " &";
        }
        table = table.substring(0, table.length() - 1) + "\\\\";
        table2 = table2.substring(0, table2.length() - 1) + "\\\\";

        data = data + "\npGEH5,";
        table = table + "\npGEH5 &";
        table2 = table2 + "\npGEH5 &";
        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + maiorDoArray(percGehAbaixo5.get(i)) + ",";
            table = table + " " + df3.format(maiorDoArray(percGehAbaixo5.get(i))) + " &";
            table2 = table2 + " " + df3.format(maiorDoArray(percGehAbaixo5.get(i))) + " &";
        }
        table = table.substring(0, table.length() - 1) + "\\\\";
        table2 = table2.substring(0, table2.length() - 1) + "\\\\";

        data = data + "\nR2_odm,";
        table = table + "\nR2 odm &";
        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + maiorDoArray(r2odm.get(i)) + ",";
            table = table + " " + df3.format(maiorDoArray(r2odm.get(i))) + " &";
        }
        table = table.substring(0, table.length() - 1) + "\\\\";

        data = data + "\nR2_links,";
        table = table + "\nR2 links &";
        table2 = table2 + "\nR2 links &";
        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + maiorDoArray(r2links.get(i)) + ",";
            table = table + " " + df3.format(maiorDoArray(r2links.get(i))) + " &";
            table2 = table2 + " " + df3.format(maiorDoArray(r2links.get(i))) + " &";
        }
        table = table.substring(0, table.length() - 1) + " \\\\ \\hline\n\\end{tabular}\n\\end{table}";
        table2 = table2.substring(0, table2.length() - 1) + " \\\\ \\hline\n\\end{tabular}\n\\end{table}";

        data = data + "\n\n\nx,x,Minimo,Media,Mediana,Maximo,Desvio padrao,ShapiroWilk,\n";

        salvarTxt("resultsAlgs lateX table.txt", table + "\n\n" + table2);

        //todos os mae
        for (int i = 0; i < getNomeDados().size(); i++) {

            /*if (!getNomeDados().get(i).substring(0, 3).equals("ALGEB".substring(0, 3))) {
                data = data + getNomeDados().get(i) + ", MAE inicial, " + stats(getMaeInicial().get(i)) + ",";
                for (int z = 0; z < getMae().get(i).length; z++) {
                    data = data + getMaeInicial().get(i)[z] + ", ";
                }
                data = data + "\n";
            }*/
            data = data + getNomeDados().get(i) + ", MAE, " + stats(getMae().get(i)) + ",";
            for (int z = 0; z < getMae().get(i).length; z++) {
                data = data + getMae().get(i)[z] + ", ";
            }
            data = data + "\n";

        }

        //todos os rmse
        for (int i = 0; i < getNomeDados().size(); i++) {

            /*if (!getNomeDados().get(i).substring(0, 3).equals("ALGEB".substring(0, 3))) {
                data = data + getNomeDados().get(i) + ", RMSE inicial, " + stats(getRmseInicial().get(i)) + ",";
                for (int z = 0; z < getRmseInicial().get(i).length; z++) {
                    data = data + getRmseInicial().get(i)[z] + ", ";
                }
                data = data + "\n";
            }*/
            data = data + getNomeDados().get(i) + ", RMSE, " + stats(getRmse().get(i)) + ",";
            for (int z = 0; z < getRmse().get(i).length; z++) {
                data = data + getRmse().get(i)[z] + ", ";
            }
            data = data + "\n";

        }
        //data = data + "\n";

        //data = data + "\n\n";
        //todos os GEH
        for (int i = 0; i < getNomeDados().size(); i++) {

            /*if (!getNomeDados().get(i).substring(0, 3).equals("ALGEB".substring(0, 3))) {
                data = data + getNomeDados().get(i) + ", GEH inicial, " + stats(getGehInicial().get(i)) + ",";
                for (int z = 0; z < getGeh().get(i).length; z++) {
                    data = data + getGehInicial().get(i)[z] + ", ";
                }
                data = data + "\n";
            }*/
            data = data + getNomeDados().get(i) + ", GEH, " + stats(getGeh().get(i)) + ",";
            for (int z = 0; z < getGeh().get(i).length; z++) {
                data = data + getGeh().get(i)[z] + ", ";
            }
            data = data + "\n";

        }

        // data = data + "\n\n";
        //todos os PERC GEH
        for (int i = 0; i < getNomeDados().size(); i++) {

            data = data + getNomeDados().get(i) + ", pGEH5, " + stats(percGehAbaixo5.get(i)) + ",";
            for (int z = 0; z < percGehAbaixo5.get(i).length; z++) {
                data = data + this.percGehAbaixo5.get(i)[z] + ", ";
            }
            data = data + "\n";

        }

        //data = data + "\n\n";
        //todos os R2
        for (int i = 0; i < getNomeDados().size(); i++) {

//            if (!getNomeDados().get(i).substring(0, 3).equals("ALGEB".substring(0, 3))) {
            data = data + getNomeDados().get(i) + ", R2 Arcos, " + stats(getR2links().get(i)) + ",";
            for (int z = 0; z < getR2links().get(i).length; z++) {
                data = data + getR2links().get(i)[z] + ", ";
            }
            data = data + "\n";
            //          }
        }
        //data = data + "\n\n";
        for (int i = 0; i < getNomeDados().size(); i++) {

            //if (!getNomeDados().get(i).substring(0, 3).equals("ALGEB".substring(0, 3))) {
            data = data + getNomeDados().get(i) + ", R2 ODM, " + stats(getR2odm().get(i)) + ",";
            for (int z = 0; z < getR2odm().get(i).length; z++) {
                data = data + getR2odm().get(i)[z] + ", ";
            }
            data = data + "\n";
            //}
        }

        //todos os TDD
        for (int i = 0; i < getNomeDados().size(); i++) {

            data = data + getNomeDados().get(i) + ", TDD , " + stats(tdd.get(i)) + ",";
            for (int z = 0; z < tdd.get(i).length; z++) {
                data = data + this.tdd.get(i)[z] + ", ";
            }
            data = data + "\n";

        }
        //todos os RE
        for (int i = 0; i < getNomeDados().size(); i++) {

            data = data + getNomeDados().get(i) + ", RE, " + stats(re.get(i)) + ",";
            for (int z = 0; z < re.get(i).length; z++) {
                data = data + this.re.get(i)[z] + ", ";
            }
            data = data + "\n";

        }

        data = data + "\n\n";

        //infos gerais
        //0 = mae, 1 = rmse, 2 = mae final vs inicial, 3 = rmse final vs inicial, 4 = geh, 5 = r2 links, 6 = r2 odm
        data = data + ",Start,runs,end,tempoMed,pop_s,t_ger,N,sensors,,";//pv MAE InVsF,pv RMSE InVsF\n";
        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + getNomeDados().get(i) + ", " + getTempos().get(i) + ", \n";// + this.calcPValue(i, i, 3) + ", " + this.calcPValue(i, i, 4) + " \n";
        }

        data = data + "\n\n";

        //valores ANOVA
        data = data + "ANOVA\n, MAE, RMSE, GEH, pGEH5, R2 arcos, R2 ODM, TDD\n";
        data = data + "," + testeANOVA(mae, "mae") + ", " + testeANOVA(rmse, "rmse") + ", " + testeANOVA(geh, "geh") + ", " + testeANOVA(percGehAbaixo5, "geh5") + ", " + testeANOVA(r2links, "r2l") + ", " + testeANOVA(r2odm, "r2o") + ", " + testeANOVA(tdd, "tdd") + "\n\n";

        // tabela de P-value para MAE
        String tab = "MAE pvalue,";

        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
        }

        tab = tab + "\n\n";

        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(mae, d, d2) + ", ";
            }
            tab = tab + "\n ";
        }

        data = data + tab + "\n\n";

        // tabela de P-value para RMSE
        tab = "RMSE pvalue,";

        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
        }

        tab = tab + "\n\n";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(rmse, d, d2) + ", ";
            }
            tab = tab + "\n ";
        }

        data = data + tab + "\n\n";

        // tabela de P-value para GEH
        tab = "GEH pvalue,";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
        }
        tab = tab + "\n\n";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(geh, d, d2) + ", ";
            }
            tab = tab + "\n ";
        }
        data = data + tab + "\n\n";

        // tabela de P-value para pGEH5
        tab = "pGEH5 pvalue,";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
        }
        tab = tab + "\n\n";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(this.percGehAbaixo5, d, d2) + ", ";
            }
            tab = tab + "\n ";
        }
        data = data + tab + "\n\n";

        // tabela de P-value para R2 links
        tab = "R2 Arcos pvalue,";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
        }
        tab = tab + "\n\n";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(r2links, d, d2) + ", ";
            }
            tab = tab + "\n ";
        }
        data = data + tab + "\n\n";

        // tabela de P-value para R2 odm
        tab = "R2 ODM pvalue,";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
        }
        tab = tab + "\n\n";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(r2odm, d, d2) + ", ";
            }
            tab = tab + "\n ";
        }
        data = data + tab + "\n\n";

        // tabela de P-value para TDD
        tab = "TDD pvalue,";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
        }
        tab = tab + "\n\n";
        for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(tdd, d, d2) + ", ";
            }
            tab = tab + "\n ";
        }
        data = data + tab + "\n\n";

        // tabela de P-value para FITNESS não aplica, pois são medidas diferentes.
        /*for (int d = 0; d < getNomeDados().size(); d++) {
            tab = tab + getNomeDados().get(d) + ", ";
            for (int d2 = 0; d2 < getNomeDados().size(); d2++) {
                tab = tab + calcPValue(d, d2, 1) + ", ";
            }
            tab = tab + "\n ";
        }
         */
        data = data + tab + "\n\n";

        for (int i = 0; i < getNomeDados().size(); i++) {
            data = data + getNomeDados().get(i) + ", MOD, \n";
            for (int z = 0; z < getMatrizOD().get(i)[0].length; z++) {
                for (int z2 = 0; z2 < getMatrizOD().get(i)[0].length; z2++) {
                    data = data + getMatrizOD().get(i)[z][z2] + ", ";
                }
                data = data + "\n";
            }
            data = data + "\n\n";
        }

        salvarTxt("resultsAlgs" + ext + ".csv", data);
    }

    public double testeANOVA(ArrayList<Double[]> t, String tipo) {

        //System.out.println("Teste ANOVA "+tipo);   
        if (t.size() < 2) {
            return -1;
        }

        Collection<double[]> x = new ArrayList<>();

        for (int a = 0; a < t.size(); a++) {
            double[] aux = new double[t.get(a).length];

            if (t.get(a).length < 2) {
                System.out.println("ERROR: Seq n' " + a + " (" + nomeDados.get(a) + ") de " + tipo + " tem " + t.get(a).length + " valor(es) (resultsAlgoritmos.testeANOVA)");
            }

            for (int b = 0; b < t.get(a).length; b++) {
                //System.out.println("get("+a+", "+nomeDados.get(a)+")get("+b+")");
                aux[b] = t.get(a)[b];
            }
            x.add(aux);
        }

        OneWayAnova a = new OneWayAnova();
        return a.anovaPValue(x);

    }

    public resultsAlgoritmos() {

        incompleteCont = new int[500]; //limite hard code. Da erros se passar de 500 resultados.
        nomeDados = new ArrayList<>();
        tempos = new ArrayList<>();
        sensores = new ArrayList<>();
        rmse = new ArrayList<>();
        geh = new ArrayList<>();
        this.percGehAbaixo5 = new ArrayList<>();
        r2odm = new ArrayList<>();
        r2links = new ArrayList<>();
        mae = new ArrayList<>();
        tdd = new ArrayList<>();
        re = new ArrayList<>();
        fitness = new ArrayList<>();
        matrizOD = new ArrayList<>();

        gehInicial = new ArrayList<>();
        rmseInicial = new ArrayList<>();
        maeInicial = new ArrayList<>();
        fitnessInicial = new ArrayList<>();

    }

    public void addPartialResults(String nome, int nSens, Double[] rsme2, Double[] mae2, Double[] fitness2, Double[] geh2, Double[] rsmeIn, Double[] maeIn, Double[] fitnessIn, Double[] gehIn,
            double[] mod, String tempo, int momento, Double[] r2l, Double[] r2od, Double[] percGeh, Double[] tdd2, Double[] re2) {

        int ind = getNomeDados().indexOf(nome);
        int n = (int) Math.sqrt(mod.length);

        double[][] mat = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                mat[i][j] = mod[i * n + j];
            }
        }

        if (ind != -1) {
            getRmse().set(ind, rsme2);
            getMae().set(ind, mae2);
            getFitness().set(ind, fitness2);
            getGeh().set(ind, geh2);
            getPercGehAbaixo5().set(ind, percGeh);
            getR2links().set(ind, r2l);
            getR2odm().set(ind, r2od);
            getTdd().set(ind, tdd2);
            getRe().set(ind, re2);

            getRmseInicial().set(ind, rsmeIn);
            getMaeInicial().set(ind, maeIn);
            getFitnessInicial().set(ind, fitnessIn);
            getGehInicial().set(ind, gehIn);

            getMatrizOD().set(ind, mat);
            getTempos().set(ind, tempo);

        } else {
            nomeDados.add(nome);
            getSensores().add(nSens);

            getTdd().add(tdd2);
            getRe().add(re2);
            getRmse().add(rsme2);
            getMae().add(mae2);
            getGeh().add(geh2);
            getPercGehAbaixo5().add(percGeh);
            getR2links().add(r2l);
            getR2odm().add(r2od);
            getGehInicial().add(gehIn);
            getFitness().add(fitness2);
            getRmseInicial().add(rsmeIn);
            getMaeInicial().add(maeIn);
            getFitnessInicial().add(fitnessIn);

            getMatrizOD().add(mat);
            getTempos().add(tempo);
            ind = getNomeDados().indexOf(nome);
        }

        System.out.println("ADD: partialResults rodada " + (momento + 1) + "/" + mae2.length + ", " + nomeDados.get(ind) + " (resultado index " + ind + ")");

        incompleteCont[ind] = momento + 1;

    }

    public void addResultados(String nome, int nSens, Double[] rsme2, Double[] mae2, Double[] fitness2, Double[] geh2,
            Double[] rsmeIn, Double[] maeIn, Double[] fitnessIn, Double[] gehIn, double[] mod, String tempo, Double[] r2l, Double[] r2od, Double[] percGeh, Double[] tdd2, Double[] re2) {
        int ind = getNomeDados().indexOf(nome);
        int n = (int) Math.sqrt(mod.length);

        double[][] mat = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                mat[i][j] = mod[i * n + j];
            }
        }

        if (ind != -1) {
            getRmse().set(ind, rsme2);
            getMae().set(ind, mae2);
            getFitness().set(ind, fitness2);
            getGeh().set(ind, geh2);
            percGehAbaixo5.set(ind, percGeh);
            getR2links().set(ind, r2l);
            getR2odm().set(ind, r2od);
            getTdd().set(ind, tdd2);
            getRe().set(ind, re2);

            getRmseInicial().set(ind, rsmeIn);
            getMaeInicial().set(ind, maeIn);
            getFitnessInicial().set(ind, fitnessIn);
            getGehInicial().set(ind, gehIn);

            getMatrizOD().set(ind, mat);
            getTempos().set(ind, tempo);

        } else {
            nomeDados.add(nome);
            getSensores().add(nSens);

            getRmse().add(rsme2);
            getMae().add(mae2);
            getGeh().add(geh2);
            percGehAbaixo5.add(percGeh);
            getR2links().add(r2l);
            getR2odm().add(r2od);
            getFitness().add(fitness2);
            getRmseInicial().add(rsmeIn);
            getMaeInicial().add(maeIn);
            getGehInicial().add(gehIn);
            getFitnessInicial().add(fitnessIn);
            getTdd().add(tdd2);
            getRe().add(re2);

            getMatrizOD().add(mat);
            getTempos().add(tempo);
            ind = getNomeDados().indexOf(nome);
        }

        incompleteCont[ind] = rmse.get(ind).length;

    }

    public void gerarBoxPlot(String ext) {

        GeraGraficos gr = new GeraGraficos(2500, 600);
        if (getRmse().size() == getNomeDados().size()) {
            gr.gerarBoxPlot(getRmse(), getNomeDados(), "RMSE", "Algorithms RMSE BoxPlot", ext);

            for (int n = 0; n < nomeDados.size(); n++) {

                if (!Objects.equals(getRmse().get(n)[0], getRmse().get(n)[1])) {//(menorDoArray(getRmse().get(n))!=maiorDoArray(getRmse().get(n))  ){
                    gr = new GeraGraficos(600, 300);
                    gr.criarHistogramaD("Graficos", "Histograma RMSE " + nomeDados.get(n), getRmse().get(n), 7);
                }
            }

        }
        if (getMae().size() == getNomeDados().size()) {
            gr.gerarBoxPlot(getMae(), getNomeDados(), "MAE", "Algorithms MAE BoxPlot", ext);

            for (int n = 0; n < nomeDados.size(); n++) {
                if (!Objects.equals(getRmse().get(n)[0], getRmse().get(n)[1])) {//if(menorDoArray(getMae().get(n))!=maiorDoArray(getMae().get(n))){
                    gr = new GeraGraficos(600, 300);
                    gr.criarHistogramaD("Graficos", "Histograma MAE " + nomeDados.get(n), getMae().get(n), 7);
                }

            }

        }
        if (getGeh().size() == getNomeDados().size()) {
            gr.gerarBoxPlot(getGeh(), getNomeDados(), "GEH", "Algorithms GEH BoxPlot", ext);

            for (int n = 0; n < nomeDados.size(); n++) {

                if (!Objects.equals(getRmse().get(n)[0], getRmse().get(n)[1])) {
                    gr = new GeraGraficos(600, 300);
                    gr.criarHistogramaD("Graficos", "Histograma GEH " + nomeDados.get(n), getGeh().get(n), 7);
                }

            }

        }
        if (this.percGehAbaixo5.size() == getNomeDados().size()) {
            gr.gerarBoxPlot(percGehAbaixo5, getNomeDados(), "Perc GEH", "Algorithms Perc GEH BoxPlot", ext);
        }

        if (getR2links().size() == getNomeDados().size()) {
            gr.gerarBoxPlot(getR2links(), getNomeDados(), "R2", "Algorithms Links R2 BoxPlot", ext);
        }
        if (getR2odm().size() == getNomeDados().size()) {
            gr.gerarBoxPlot(getR2odm(), getNomeDados(), "R2", "Algorithms ODME R2 BoxPlot", ext);
        }

        if (getR2odm().size() == getNomeDados().size()) {
            gr.gerarBoxPlot(tdd, getNomeDados(), "TDD", "Algorithms ODME TDD BoxPlot", ext);
        }
        if (getR2odm().size() == getNomeDados().size()) {
            gr.gerarBoxPlot(re, getNomeDados(), "RE", "Algorithms ODME RE BoxPlot", ext);
        }

    }

    public void salvarArquivo(boolean salvaCsv) {
        try {
            FileOutputStream arquivoGrav = new FileOutputStream(filename);
            ObjectOutputStream objGravar = new ObjectOutputStream(arquivoGrav);
            objGravar.writeObject(this);
            objGravar.flush();
            objGravar.close();
            arquivoGrav.flush();
            arquivoGrav.close();
            System.out.println("OK: Arquivo '" + filename + "' salvo!");
        } catch (IOException e) {
            //  e.printStackTrace();
            System.out.println("ERROR: Falha ao salvar " + filename);
        }

        if (salvaCsv) {
            if (getNomeDados().size() > 0) {

                boolean tudoCompleto = true;

                for (int n = 0; n < nomeDados.size(); n++) {
                    if (incompleteCont[n] < rmse.get(n).length) {
                        System.out.println("ALERT: " + nomeDados.get(n) + " incompleto: (" + incompleteCont[n] + "/" + rmse.get(n).length + ") - Não gera CSV de dados");
                        tudoCompleto = false;
                        return;
                    }
                }

                gerarBoxPlot("");
                salvarCSVDados("");

                gerarGraficoLinhaNumSensores();
            }
        }

    }

    public resultsAlgoritmos recuperarArquivo() {
        resultsAlgoritmos x = new resultsAlgoritmos();
        try {

            try (FileInputStream arquivoLeitura = new FileInputStream(filename)) {
                ObjectInputStream objLeitura = new ObjectInputStream(arquivoLeitura);

                x = (resultsAlgoritmos) objLeitura.readObject();
                objLeitura.close();

            }
            System.out.println("OK: Recuperou objeto de '" + filename + "'! (" + x.getNomeDados().size() + " resultados)\n");
        } catch (IOException | ClassNotFoundException e) {
            // e.printStackTrace();
            System.out.println("ALERT: Não recuperou registros de arquivo '" + filename + "'");
        }
        return x;
    }

    private boolean salvarTxt(String name, String conteudo) {

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(name);
            fileWriter.append(conteudo);
        } catch (IOException e) {
            System.out.println("ERROR: FileWriter de '" + name + "'.");
            //  e.printStackTrace();
            return false;
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
                return true;
            } catch (IOException e) {
                System.out.println("ERROR: While flushing/closing fileWriter de '" + name + "'.");
                //  e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * @return the nomeDados
     */
    public ArrayList<String> getNomeDados() {
        return nomeDados;
    }

    /**
     * @return the tempos
     */
    public ArrayList<String> getTempos() {
        return tempos;
    }

    /**
     * @return the matrizOD
     */
    public ArrayList<double[][]> getMatrizOD() {
        return matrizOD;
    }

    public double[] getFlatMatrizOD(int ind) {

        double[] x = new double[matrizOD.get(ind).length * matrizOD.get(ind).length];

        for (int o = 0; o < matrizOD.get(ind).length; o++) {
            for (int d = 0; d < matrizOD.get(ind).length; d++) {
                x[o * matrizOD.get(ind).length + d] = matrizOD.get(ind)[o][d];
            }
        }

        return x;
    }

    public double getMenorCusto(int ind) {
        double c = 999999999;

        for (int x = 0; x < this.incompleteCont[ind]; x++) {
            if (fitness.get(ind)[x] < c) {
                c = fitness.get(ind)[x];
            }
        }

        return c;
    }

    /**
     * @return the rmse
     */
    public ArrayList<Double[]> getRmse() {
        return rmse;
    }

    public ArrayList<Double[]> getGeh() {
        return geh;
    }

    /**
     * @return the mae
     */
    public ArrayList<Double[]> getMae() {
        return mae;
    }

    /**
     * @return the fitness
     */
    public ArrayList<Double[]> getFitness() {
        return fitness;
    }

    /**
     * @return the rmseInicial
     */
    public ArrayList<Double[]> getRmseInicial() {
        return rmseInicial;
    }

    public ArrayList<Double[]> getGehInicial() {
        return gehInicial;
    }

    /**
     * @return the maeInicial
     */
    public ArrayList<Double[]> getMaeInicial() {
        return maeInicial;
    }

    /**
     * @return the fitnessInicial
     */
    public ArrayList<Double[]> getFitnessInicial() {
        return fitnessInicial;
    }

    public double getMaiorFitness(int ind) {
        double c = -999999999;

        for (int x = 0; x < this.incompleteCont[ind]; x++) {
            if (fitness.get(ind)[x] < c) {
                c = fitness.get(ind)[x];
            }
        }

        return c;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the r2links
     */
    public ArrayList<Double[]> getR2links() {
        return r2links;
    }

    /**
     * @return the r2odm
     */
    public ArrayList<Double[]> getR2odm() {
        return r2odm;
    }

    /**
     * @return the sensores
     */
    public ArrayList<Integer> getSensores() {
        return sensores;
    }

    public void gerarGraficoLinhaNumSensores() {

        if (nomeDados.size() < 12) {
            System.out.println("CANCEL: Apenas " + nomeDados.size() + " registros. Poucos para variacao de numero de sensores.");
            return;
        }

        boolean incluirMaxNaoDeterm = true;
        int largura = 800;
        int altura = 350;

        GeraGraficos g = new GeraGraficos(largura, altura);

        ArrayList<String> metricas = new ArrayList<>();
        ArrayList<String> algoritmos = new ArrayList<>();
        ArrayList<String> algoritmos2 = new ArrayList<>(); //nao determin.
        ArrayList<Integer> pontos = new ArrayList<>();
        ArrayList<Double> labelValues = new ArrayList<>();
        ArrayList<String> labelSeries = new ArrayList<>();
        ArrayList<String> labelSeries2 = new ArrayList<>();

        ArrayList<Integer> naoDeterm = new ArrayList<>();

        //identificar quantidade de algoritmos diferentes
        for (int n = 0; n < nomeDados.size(); n++) {
            String x = nomeDados.get(n).split("_")[0];
            if (!algoritmos.contains(x) && !x.equals("MOD")) {
                algoritmos.add(x);

                if (menorDoArray(geh.get(n)) != maiorDoArray(geh.get(n)) && incluirMaxNaoDeterm) {
                    //contNaoDeterministicos++;
                    algoritmos2.add(x + "_Med");
                    naoDeterm.add(algoritmos.size() - 1);
                }

            }
            if (!pontos.contains(sensores.get(n)) && sensores.get(n) > 0) {
                pontos.add(sensores.get(n));
                labelValues.add(1.0 * sensores.get(n));
            }
        }

        //mae, rmse, geh, r2 odm, r2 arcos, tdd
        metricas.add("MAE");
        metricas.add("RMSE");
        metricas.add("GEH");
        metricas.add("R2 ODM");
        metricas.add("R2 Arcos");
        metricas.add("TDD");

        System.out.println("INF: naoDeterm.size = " + naoDeterm.size());

        ArrayList<Double>[] desvios = new ArrayList[metricas.size()];
        Double[] maioresDesvios = new Double[metricas.size()];
        ArrayList<Double>[] desvios2 = new ArrayList[metricas.size()];
        Double[] maioresDesvios2 = new Double[metricas.size()];

        ArrayList<Double>[] gehD = new ArrayList[algoritmos.size()];
        ArrayList<Double>[] percgehD = new ArrayList[algoritmos.size()];
        ArrayList<Double>[] r2linkD = new ArrayList[algoritmos.size()];

        ArrayList<Double>[] tddD = new ArrayList[algoritmos.size() + algoritmos2.size()];
        ArrayList<Double>[] reD = new ArrayList[algoritmos.size() + algoritmos2.size()];
        ArrayList<Double>[] maeD = new ArrayList[algoritmos.size() + algoritmos2.size()];
        ArrayList<Double>[] rmseD = new ArrayList[algoritmos.size() + algoritmos2.size()];
        ArrayList<Double>[] r2odmD = new ArrayList[algoritmos.size() + algoritmos2.size()];

        for (int a = 0; a < metricas.size(); a++) {
            desvios[a] = new ArrayList<>();
            maioresDesvios[a] = 0.0;
            desvios2[a] = new ArrayList<>();
            maioresDesvios2[a] = 0.0;
        }

        for (int a = 0; a < algoritmos.size(); a++) {
            tddD[a] = new ArrayList<>();
            reD[a] = new ArrayList<>();
            maeD[a] = new ArrayList<>();
            rmseD[a] = new ArrayList<>();
            gehD[a] = new ArrayList<>();
            percgehD[a] = new ArrayList<>();
            r2odmD[a] = new ArrayList<>();
            r2linkD[a] = new ArrayList<>();

            if (!algoritmos.get(a).equals("ALGEB")) {

                if (!algoritmos.get(a).equals("GAg")) {
                    if (!algoritmos.get(a).equals("PSOg")) {
                        labelSeries.add(algoritmos.get(a));
                        labelSeries2.add(algoritmos.get(a));
                    } else {
                        labelSeries.add("PSO");
                        labelSeries2.add("PSO");
                    }

                } else {
                    labelSeries.add("GA");
                    labelSeries2.add("GA");
                }

            } else {
                labelSeries.add("LS");
                labelSeries2.add("LS");
            }

        }

        for (int a = 0; a < algoritmos2.size(); a++) {

            tddD[algoritmos.size() + a] = new ArrayList<>();
            reD[algoritmos.size() + a] = new ArrayList<>();
            maeD[algoritmos.size() + a] = new ArrayList<>();
            rmseD[algoritmos.size() + a] = new ArrayList<>();
            r2odmD[algoritmos.size() + a] = new ArrayList<>();

            labelSeries2.add(algoritmos2.get(a));

        }

        //tudo pronto, hora de adicionar os dados
        ArrayList<Integer> iAlg = new ArrayList<>();
        ArrayList<Integer> iPonto = new ArrayList<>();
        for (int n = 0; n < nomeDados.size(); n++) {
            int indAlg = algoritmos.indexOf(nomeDados.get(n).split("_")[0]);
            int indP = pontos.indexOf(sensores.get(n));
            iAlg.add(indAlg);
            iPonto.add(indP);
        }

        for (int p = 0; p < pontos.size(); p++) {
            for (int n = 0; n < nomeDados.size(); n++) { // todos os registros, algoritmo X num sensores

                if (iAlg.get(n) != -1) {
                    if (iPonto.get(n) == p) {

                        if (naoDeterm.contains(iAlg.get(n))) {

                            //System.out.println("INF: Encontrou nao determ. "+iAlg.get(n));
                            int i = algoritmos2.indexOf(nomeDados.get(n).split("_")[0] + "_Med") + algoritmos.size();  //WCS

                            tddD[i].add(g.getCorrespMenorGEH(geh.get(n), tdd.get(n)));//tddD[i].add(maiorDoArray(tdd.get(n)));
                            //reD[i].add(g.getCorrespMenorGEH(geh.get(n), re.get(n)));//reD[i].add(maiorDoArray(re.get(n)));
                            r2odmD[i].add(g.getCorrespMenorGEH(geh.get(n), r2odm.get(n)));//r2odmD[i].add(menorDoArray(r2odm.get(n)));
                            maeD[i].add(g.getCorrespMenorGEH(geh.get(n), mae.get(n)));//maeD[i].add(maiorDoArray(mae.get(n)));
                            rmseD[i].add(g.getCorrespMenorGEH(geh.get(n), rmse.get(n)));//rmseD[i].add(maiorDoArray(rmse.get(n))); 

                            /*tddD[i].add(g.getMedianaArray(tdd.get(n)));//tddD[i].add(maiorDoArray(tdd.get(n)));
                        //reD[i].add(g.getCorrespMenorGEH(geh.get(n), re.get(n)));//reD[i].add(maiorDoArray(re.get(n)));
                        r2odmD[i].add(g.getMedianaArray(r2odm.get(n)));//r2odmD[i].add(menorDoArray(r2odm.get(n)));
                        maeD[i].add(g.getMedianaArray(mae.get(n)));//maeD[i].add(maiorDoArray(mae.get(n)));
                        rmseD[i].add(g.getMedianaArray(rmse.get(n)));//rmseD[i].add(maiorDoArray(rmse.get(n))); */
                            if (nomeDados.get(n).split("_")[0].equals("PSO") || nomeDados.get(n).split("_")[0].equals("PSOg") || nomeDados.get(n).split("_")[0].equals("PSOf")) {

                                // System.out.println("INF: Encontrou nao determ. "+iAlg.get(n));
                                //mae, rmse, geh, r2 odm, r2 arcos, tdd
                                if (g.getDesvioPadrao(mae.get(n)) > maioresDesvios[0]) {
                                    maioresDesvios[0] = g.getDesvioPadrao(mae.get(n));
                                }
                                desvios[0].add(g.getDesvioPadrao(mae.get(n)));

                                if (g.getDesvioPadrao(rmse.get(n)) > maioresDesvios[1]) {
                                    maioresDesvios[1] = g.getDesvioPadrao(rmse.get(n));
                                }
                                desvios[1].add(g.getDesvioPadrao(rmse.get(n)));

                                if (g.getDesvioPadrao(geh.get(n)) > maioresDesvios[2]) {
                                    maioresDesvios[2] = g.getDesvioPadrao(geh.get(n));
                                }
                                desvios[2].add(g.getDesvioPadrao(geh.get(n)));

                                if (g.getDesvioPadrao(r2odm.get(n)) > maioresDesvios[3]) {
                                    maioresDesvios[3] = g.getDesvioPadrao(r2odm.get(n));
                                }
                                desvios[3].add(g.getDesvioPadrao(r2odm.get(n)));

                                if (g.getDesvioPadrao(r2links.get(n)) > maioresDesvios[4]) {
                                    maioresDesvios[4] = g.getDesvioPadrao(r2links.get(n));
                                }
                                desvios[4].add(g.getDesvioPadrao(r2links.get(n)));

                                if (g.getDesvioPadrao(tdd.get(n)) > maioresDesvios[5]) {
                                    maioresDesvios[5] = g.getDesvioPadrao(tdd.get(n));
                                }
                                desvios[5].add(g.getDesvioPadrao(tdd.get(n)));
                            } else if (nomeDados.get(n).split("_")[0].equals("GA") || nomeDados.get(n).split("_")[0].equals("GAg") || nomeDados.get(n).split("_")[0].equals("GAf")) {

                                //System.out.println("INF: Encontrou nao determ. "+iAlg.get(n));
                                //mae, rmse, geh, r2 odm, r2 arcos, tdd
                                if (g.getDesvioPadrao(mae.get(n)) > maioresDesvios2[0]) {
                                    maioresDesvios2[0] = g.getDesvioPadrao(mae.get(n));
                                }
                                desvios2[0].add(g.getDesvioPadrao(mae.get(n)));

                                if (g.getDesvioPadrao(rmse.get(n)) > maioresDesvios2[1]) {
                                    maioresDesvios2[1] = g.getDesvioPadrao(rmse.get(n));
                                }
                                desvios2[1].add(g.getDesvioPadrao(rmse.get(n)));

                                if (g.getDesvioPadrao(geh.get(n)) > maioresDesvios2[2]) {
                                    maioresDesvios2[2] = g.getDesvioPadrao(geh.get(n));
                                }
                                desvios2[2].add(g.getDesvioPadrao(geh.get(n)));

                                if (g.getDesvioPadrao(r2odm.get(n)) > maioresDesvios2[3]) {
                                    maioresDesvios2[3] = g.getDesvioPadrao(r2odm.get(n));
                                }
                                desvios2[3].add(g.getDesvioPadrao(r2odm.get(n)));

                                if (g.getDesvioPadrao(r2links.get(n)) > maioresDesvios2[4]) {
                                    maioresDesvios2[4] = g.getDesvioPadrao(r2links.get(n));
                                }
                                desvios2[4].add(g.getDesvioPadrao(r2links.get(n)));

                                if (g.getDesvioPadrao(tdd.get(n)) > maioresDesvios2[5]) {
                                    maioresDesvios2[5] = g.getDesvioPadrao(tdd.get(n));
                                }
                                desvios2[5].add(g.getDesvioPadrao(tdd.get(n)));
                            }

                        }

                        tddD[iAlg.get(n)].add(g.getMedianaArray(tdd.get(n)));//tddD[iAlg.get(n)].add(menorDoArray(tdd.get(n)));
                        //reD[iAlg.get(n)].add(menorDoArray(re.get(n)));
                        r2odmD[iAlg.get(n)].add(g.getMedianaArray(r2odm.get(n)));//r2odmD[iAlg.get(n)].add(maiorDoArray(r2odm.get(n)));
                        maeD[iAlg.get(n)].add(g.getMedianaArray(mae.get(n)));//maeD[iAlg.get(n)].add(menorDoArray(mae.get(n)));
                        rmseD[iAlg.get(n)].add(g.getMedianaArray(rmse.get(n)));//rmseD[iAlg.get(n)].add(menorDoArray(rmse.get(n)));

                        gehD[iAlg.get(n)].add(menorDoArray(geh.get(n)));
                        percgehD[iAlg.get(n)].add(maiorDoArray(percGehAbaixo5.get(n)));
                        r2linkD[iAlg.get(n)].add(maiorDoArray(r2links.get(n)));
                    }

                }

            }

            for (int a = 0; a < algoritmos.size(); a++) {

                if (tddD[a].size() <= p) {
                    tddD[a].add(0.0);
                }
                if (reD[a].size() <= p) {
                    reD[a].add(0.0);
                }

                if (maeD[a].size() <= p) {
                    maeD[a].add(0.0);
                }
                if (rmseD[a].size() <= p) {
                    rmseD[a].add(0.0);
                }
                if (gehD[a].size() <= p) {
                    gehD[a].add(0.0);
                }
                if (percgehD[a].size() <= p) {
                    percgehD[a].add(0.0);
                }
                if (r2odmD[a].size() <= p) {
                    r2odmD[a].add(0.0);
                }
                if (r2linkD[a].size() <= p) {
                    r2linkD[a].add(0.0);
                }

            }

        }

        for (int d = 0; d < metricas.size(); d++) {
            for (int x = 0; x < desvios[d].size(); x++) {
                desvios[d].set(x, desvios[d].get(x) / maioresDesvios[d]);
                desvios2[d].set(x, desvios2[d].get(x) / maioresDesvios2[d]);
            }
        }

        /*for(int x=arestasSensorI;x<=arestasSensorF;x+=step){
             data[0].add(runAlgebricSolution(ODmatrix1,virtualSensors1,clusters,tempoPriori,tempoProblema,x));
             labelValues.add((double)x);
         }*/
        g.GeraGraficosLinha("Graficos", "R2 ODM vs Number of Sensors", r2odmD, labelValues, labelSeries2,
                "Número de sensores", "R2 ODM");
        g = new GeraGraficos(largura, altura);
        g.GeraGraficosLinha("Graficos", "MAE vs Number of Sensors", maeD, labelValues, labelSeries2,
                "Número de sensores", "MAE");
        g = new GeraGraficos(largura, altura);
        g.GeraGraficosLinha("Graficos", "RMSE vs Number of Sensors", rmseD, labelValues, labelSeries2,
                "Número de sensores", "RMSE");
        g = new GeraGraficos(largura, altura);
        g.GeraGraficosLinha("Graficos", "TDD vs Number of Sensors", tddD, labelValues, labelSeries2,
                "Número de sensores", "TDD");

        g = new GeraGraficos(largura, altura);
        g.GeraGraficosLinha("Graficos", "GEH vs Number of Sensors", gehD, labelValues, labelSeries,
                "Número de sensores", "GEH");
        g = new GeraGraficos(largura, altura);
        g.GeraGraficosLinha("Graficos", "Perc GEH 5 vs Number of Sensors", percgehD, labelValues, labelSeries,
                "Número de sensores", "pGEH5");
        g = new GeraGraficos(largura, altura);
        g.GeraGraficosLinha("Graficos", "R2 Links vs Number of Sensors", r2linkD, labelValues, labelSeries,
                "Número de sensores", "R2 Arcos");

        if (incluirMaxNaoDeterm) {

            g = new GeraGraficos(largura, altura);
            g.GeraGraficosLinha("Graficos", "Desvio padrão normalizado do PSO", desvios, labelValues, metricas,
                    "Número de sensores", "Desvio padrão normalizao");

            g = new GeraGraficos(largura, altura);
            g.GeraGraficosLinha("Graficos", "Desvio padrão normalizado do GA", desvios2, labelValues, metricas,
                    "Número de sensores", "Desvio padrão normalizado");

        }

        /*g = new GeraGraficos(1000,500);
        g.GeraGraficosLinha("Graficos", "RE vs Number of Sensors", reD, labelValues, labelSeries,
                "Sensors number", "RE");*/
    }

    public double menorDoArray(Double[] a) {
        double m = 90000;
        //System.out.println("Array a.lenght = "+a.length);
        for (int x = 0; x < a.length; x++) {
            //  System.out.println("a["+x+"]="+a[x]);

            if (a[x] == null) {
                if (x == 0) {
                    System.out.println("ERROR: a[" + x + "] == null  (resultsAlgoritmos.menorDoArray)");
                }
            } else if (a[x] < m && a[x] > 0) {
                m = a[x];
            }
        }
        return m;
    }

    public double maiorDoArray(Double[] a) {
        double m = -90000;
        for (int x = 0; x < a.length; x++) {
            if (a[x] == null) {
                if (x == 0) {
                    System.out.println("ERROR: a[" + x + "] == null");
                }
            } else if (a[x] > m && a[x] > 0) {
                m = a[x];
            }
        }
        return m;
    }

    /**
     * @return the percGehAbaixo5
     */
    public ArrayList<Double[]> getPercGehAbaixo5() {
        return percGehAbaixo5;
    }

    /**
     * @return the tdd
     */
    public ArrayList<Double[]> getTdd() {
        return tdd;
    }

    /**
     * @return the re
     */
    public ArrayList<Double[]> getRe() {
        return re;
    }

}
