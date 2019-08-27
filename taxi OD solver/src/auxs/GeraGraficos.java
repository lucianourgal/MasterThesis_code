/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author lucia
 */
public class GeraGraficos {

    XYSeriesCollection xydata = new XYSeriesCollection();

    int largura;// = 1000;
    int altura;// = 800;
    boolean printScatterInfo = true;
    String folder = "Graphics";

    public void criarScatter(String filename, String Xlabel, String yLabel, double[]v1, double[] v2) {

        this.criaDiretorio(folder);
        folder = folder+"\\";
        
        if(printScatterInfo){
        System.out.println("PROC 2 vetores de "+v1.length+" pos: "+filename+ "; '"+Xlabel+"' e '"+yLabel+"' (GeraGraficos.criarScatter)");
        //this.printStats(v1, Xlabel);
        //this.printStats(v2, yLabel);
        double [] dif = new double[v2.length];
        for(int a=0;a<v2.length;a++)
            dif[a] = v1[a]-v2[a];
        printStats(dif,"Diff");
        }
        
        
        XYSeriesCollection result = new XYSeriesCollection();
        XYSeries series = new XYSeries("");
        for (int i = 0; i < v1.length; i++) {
            double x = v1[i];
            double y = v2[i];
            series.add(x, y);
        }
        result.addSeries(series);

        JFreeChart chart = ChartFactory.createScatterPlot(
                filename, // chart title
                Xlabel, // x axis label
                yLabel, // y axis label
                result, // data  ***-----PROBLEM------***
                PlotOrientation.VERTICAL,
                false, // include legend
                true, // tooltips
                false // urls
        );
        
        try {
            ChartUtilities.saveChartAsPNG(new File(folder+filename  + ".png"), chart, largura, altura);
        } catch (IOException ex) {
            Logger.getLogger(GeraGraficos.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void gerarBoxPlot(ArrayList<Double[]> values, final ArrayList<String> nomeDados, String tituloEixoY, String fileName, String ex2) {

        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        ArrayList<Number> list;

        for (int x = 0; x < nomeDados.size(); x++) {
            if(!nomeDados.get(x).split("_")[0].equals("MOD")){
            list = new ArrayList<>();
            list.addAll(Arrays.asList(values.get(x)));
            dataset.add(list, nomeDados.get(x), nomeDados.get(x));
            }
        }

        CategoryAxis xAxis = new CategoryAxis("");
        NumberAxis yAxis = new NumberAxis(tituloEixoY);
        yAxis.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(false);
        renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis,
                renderer);

        JFreeChart chart = new JFreeChart(fileName,
                new Font("SansSerif", Font.BOLD, 14), plot, true);

        try {
            
            this.criaDiretorio(folder);
            
            ChartUtilities.saveChartAsPNG(new File(folder+"\\"+fileName + ex2 + ".png"), chart, largura, altura);
        } catch (IOException ex) {
            Logger.getLogger(GeraGraficos.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public GeraGraficos(int larg, int alt) {
        largura = larg;
        altura = alt;
    }
    
    public GeraGraficos() {
        largura = 2000;
        altura = 1000;
    }


    public void GeraCDF2_From_1_DescpStats(String pasta, String nome, DescriptiveStatistics ds, ArrayList<String> nomes) {

        ArrayList<Double>[] data;
        ArrayList<Double> labelValues;

        data = new ArrayList[1];
        labelValues = new ArrayList<>();

        int x = 0;

        data[x] = new ArrayList<>();
        data[x].add(0.0);
        labelValues.add(ds.getMin());

        for (double a = 0.5; a < 100; a = a + 0.5) {
            //data[x].add(ds.getPercentile(a));
            data[x].add(a);
            //if(x!=0)
            labelValues.add(ds.getPercentile(a));//labelValues.add(a*1.0);
        }

        //data[x].add(100.0);
        //labelValues.add(ds.getMax());//labelValues.add(a*1.0)
        GeraGraficosLinha(pasta, nome, data, labelValues, nomes, "", "");
    }

    public void GeraCDF_FromDescpStats(String pasta, String nome, ArrayList<DescriptiveStatistics> ds, ArrayList<String> nomes) {

        ArrayList<Double>[] data;
        ArrayList<Double> labelValues;

        data = new ArrayList[ds.size()];
        labelValues = new ArrayList<>();

        //encontrar maior valor
        double maior = 0;
        int maiorSerie = -1;
        for (int x = 0; x < ds.size(); x++) {
            if (ds.get(x).getPercentile(99) > maior) {
                maior = ds.get(x).getPercentile(99);
                maiorSerie = x;
            }
        }
        data[maiorSerie] = new ArrayList<>();
        for (double a = 0.5; a < 99.5; a = a + 0.5) {
            labelValues.add(ds.get(maiorSerie).getPercentile(a));
            data[maiorSerie].add((double) a);
        }
        double find;
        boolean found;
        for (int x = 0; x < ds.size(); x++) {

            if (x != maiorSerie) {
                data[x] = new ArrayList<>();
                data[x].add(ds.get(x).getMin());

                for (double a = 0.5; a < 99.5; a = a + 0.5) {
                    find = ds.get(maiorSerie).getPercentile(a);
                    found = false;
                    for (double b = 0.3; b < 99.5; b = b + 0.3) {
                        if (ds.get(x).getPercentile(b) >= find) {
                            data[x].add((double) b);
                            b = 100; //sai do loop se já encontrou
                            found = true;
                        }
                    }
                    if (!found) {
                        data[x].add(100.0);
                    }
                }

            }
        }

        GeraGraficosLinha(pasta, nome, data, labelValues, nomes, "", "");
    }

    boolean usarNomeEmCimaGrafLinha = false;
    
    public void setUseNomeEmGraficoLinha(boolean xc){
       this.usarNomeEmCimaGrafLinha = xc;
    }
    
    
    public void GeraGraficosLinha(String pasta, String nome, ArrayList<Double>[] data, ArrayList<Double> labelValues,
            ArrayList<String> labelSeries, String labelH, String labelV) {

        this.criaDiretorio(pasta);
        DefaultCategoryDataset line_chart = new DefaultCategoryDataset();
        

        XYSeries[] x5 = new XYSeries[labelSeries.size()];

        for (int x = 0; x < labelSeries.size(); x++) {
            x5[x] = new XYSeries(labelSeries.get(x));
        }

        for (int a = 0; a < labelValues.size(); a++) {

            for (int x = 0; x < labelSeries.size(); x++) {
                if(data[x].size()<labelValues.size())
                    System.out.println("ERROR: "+nome+" - data["+x+", "+labelSeries.get(x)+"].size = "+data[x].size()+" < "
                            + "labelValues.size = "+labelValues.size()+"; labelSeries.size = "+labelSeries.size()+"  (GeraGraficos.GeraGraficosLinha)");
                line_chart.addValue(data[x].get(a), labelSeries.get(x), labelValues.get(a));
            }

            try {
                for (int x = 0; x < labelSeries.size(); x++) {
                    x5[x].add(labelValues.get(a), data[x].get(a));
                }

            } catch (Exception e) {
                System.out.println("ERRO: Ao adicionar elemento em XYSeries (GeraGraficos)");
            }

        }

        for (int x = 0; x < labelSeries.size(); x++) {
            xydata.addSeries(x5[x]);
        }

        try {
            /* Step -2:Define the JFreeChart object to create line chart */
            
            JFreeChart lineChartObject;
            
            if(usarNomeEmCimaGrafLinha)
                lineChartObject = ChartFactory.createXYLineChart(nome, labelH, labelV, xydata, PlotOrientation.VERTICAL, true, true, false);
            else
                lineChartObject = ChartFactory.createXYLineChart("", labelH, labelV, xydata, PlotOrientation.VERTICAL, true, true, false);
           
            lineChartObject.setBorderPaint(Color.white);
            // CategoryPlot plot = (CategoryPlot) lineChartObject.getPlot();
            XYPlot plot = lineChartObject.getXYPlot();
            //plot.getRenderer().setSeriesPaint(2, Color.GREEN);

            ChartUtilities.saveChartAsPNG(new File(pasta + "\\" + nome + ".png"), lineChartObject, largura, altura);
            System.out.println("OK: " + pasta + " " + nome + ".png gerado! "+horaAtual());

        } catch (IOException i) {
            System.out.println("ERROR: Falha ao gerar " + nome + ".png (GeraGraficos)");
        }

    }

    public void timeSeriesGraficoInt(String pasta, String nome, ArrayList<Integer> values, ArrayList<String> datas) {
        ArrayList<Double> ds = new ArrayList<>();
        for (int x = 0; x < values.size(); x++) {
            ds.add(values.get(x) * 1.0);
        }

        timeSeriesGrafico(pasta, nome, ds, datas);
    }

    public void timeSeriesGrafico(String pasta, String nome, ArrayList<Double> values, ArrayList<String> datas) {

        this.criaDiretorio(pasta);
        TimeSeries series1 = new TimeSeries("Data");
        Date date;

        for (int x = 0; x < datas.size(); x++) {
            date = new Date();
            date.setDate(Integer.parseInt(datas.get(x).split("/")[0]));
            date.setMonth(Integer.parseInt(datas.get(x).split("/")[1]));
            date.setYear(Integer.parseInt(datas.get(x).split("/")[2]));
            series1.add(new Day(date), values.get(x));

        }
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series1);

        try {
            /* Step -2:Define the JFreeChart object to create line chart */
            JFreeChart lineChartObject = ChartFactory.createXYLineChart(nome, "Tempo", "", dataset, PlotOrientation.VERTICAL, true, true, false);

            // CategoryPlot plot = (CategoryPlot) lineChartObject.getPlot();
            XYPlot plot = lineChartObject.getXYPlot();
            //plot.getRenderer().setSeriesPaint(2, Color.GREEN);

            ChartUtilities.saveChartAsPNG(new File(pasta + "\\" + nome + ".png"), lineChartObject, largura, altura);
            System.out.println("OK: " + pasta + " " + nome + ".png gerado! "+horaAtual());

        } catch (Exception i) {
            System.out.println("ERROR: Falha ao gerar " + nome + ".png (GeraGraficos)");
        }

    }

    public static String horaAtual() {
        return (new SimpleDateFormat("dd/MM, HH:mm:ss").format(Calendar.getInstance().getTime()));
    }
    
    public void criarHistogramaA(String pasta, String nome, ArrayList<Double> qtde, int numeroClasses) {

        double[] vet = new double[qtde.size()];
        for (int x = 0; x < qtde.size(); x++) {
            vet[x] = qtde.get(x);
        }

        criarHistograma(pasta, nome, vet, numeroClasses);
    }

    public void criarHistogramaI(String pasta, String nome, ArrayList<Integer> qtde, int numeroClasses) {

        double[] vet = new double[qtde.size()];
        for (int x = 0; x < qtde.size(); x++) {
            vet[x] = qtde.get(x) * 1.0;
        }

        criarHistograma(pasta, nome, vet, numeroClasses);
    }
    
     public void criarHistogramaD(String pasta, String nome, Double [] qtde, int numeroClasses) {
         System.out.println("PROC: Criando histograma "+nome+"... "+qtde.length+ " amostras.");
         
        int cont = 0;
        for (Double qtde1 : qtde) {
            if (qtde1 != null) {
                cont++;
            }
        }
            
         
        double[] vet = new double[cont];
        for (int x = 0; x < cont; x++) {
            vet[x] = qtde[x] * 1.0;
        }

        criarHistograma(pasta, nome, vet, numeroClasses);
    }
    

    public void criarHistograma(String pasta, String nome, double[] qtde, int numeroClasses) {

        this.criaDiretorio(pasta);
        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("Histogram", qtde, numeroClasses);

        String xaxis = "Classes";
        String yaxis = "Probabilidade";
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        boolean show = false;
        boolean toolTips = false;
        boolean urls = false;
        JFreeChart chart = ChartFactory.createHistogram(nome, xaxis, yaxis,
                dataset, orientation, show, toolTips, urls);

        try {
            ChartUtilities.saveChartAsPNG(new File(pasta + "\\" + nome + ".png"), chart, largura, altura);
            System.out.println("OK:" + nome + ".png gerado com " + qtde.length + " amostras!");
        } catch (IOException e) {
            System.out.println("ERROR: Falha ao gerar " + nome + ".png!");
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

    
    public void printStats(double [] ns, String name){
    DecimalFormat df2 = new DecimalFormat(".##");
    DescriptiveStatistics d = new DescriptiveStatistics();
    for(int x=0;x<ns.length;x++)
        d.addValue(ns[x]);
    
    String t = "STATS: " + name + ": Min. " + df2.format(d.getMin()) + "; Mean. " + df2.format(d.getMean()) + "; "; 

        for (int p = 5; p < 100; p = p + 10) {
            t = t + " p" + p + " " + df2.format(d.getPercentile(p)) ;
        }

        System.out.println(t+ "; Max. " + df2.format(d.getMax()) + ";");  
    }
    
    
    public double getDesvioPadrao(Double [] v){
    
    DescriptiveStatistics d = new DescriptiveStatistics();
    
    for(int x=0;x<v.length;x++)
        d.addValue(v[x]);
    
    return d.getStandardDeviation();
    }
    
    
    
    public double getMedianaArray(Double [] v){
    
    DescriptiveStatistics d = new DescriptiveStatistics();
    
    for(int x=0;x<v.length;x++)
        d.addValue(v[x]);
    
    return d.getPercentile(50);
    }
    
    public double getMediaArray(Double [] v){
    
    DescriptiveStatistics d = new DescriptiveStatistics();
    
    for(int x=0;x<v.length;x++)
        d.addValue(v[x]);
    
    return d.getMean();
    }
    
    
    public double getCorrespMenorGEH(Double [] geh, Double [] other){
    
    double m = geh[0];
    int i = 0;
    
    for(int x=1;x<geh.length;x++)
        if(geh[x]<m){
            m = geh[x];
            i = x;
        }
        
    return other[i];
    
    }
    
    
    
    
}
