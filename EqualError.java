import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.trees.*;
import weka.classifiers.bayes.*;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.Filter;
import java.util.Random;
import java.util.Enumeration;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import com.github.rcaller.rstuff.*;

public class EqualError {
    public static Instances filter_BNet(Instances orig) throws Exception {
        // Attributes to keep in instances
        int[] indices = {0,1,2,3,4,5,6,8,9,10,11,12,13,15,16,17,18,20,21,22,24,25,26,27,28,29,31,32,33,34,35,36,37,39,40,42,44,45,46,48,50,51,53,54,55,56,57,58,59,60,61,62,63,64,65,67,68,69,70,71};
        Remove remove = new Remove();
		remove.setAttributeIndicesArray(indices);
		remove.setInvertSelection(true);
		remove.setInputFormat(orig);
		return Filter.useFilter(orig, remove);
    }

    
    public static Instances filter_NB(Instances orig) throws Exception {
        // Attributes to keep in instances
        int[] indices = {0,1,2,3,4,5,6,9,10,
                         11,12,15,22,24,28,34
                         ,38,40,42,45,46,47,48
                         ,51,52,53,54,55,58,59,
                         60,61,63,65,66,68,69,70,71};
        Remove remove = new Remove();
		remove.setAttributeIndicesArray(indices);
		remove.setInvertSelection(true);
		remove.setInputFormat(orig);
		return Filter.useFilter(orig, remove);
    }

    public static Instances squareValues(Instances orig){
        // create return object wih modified data of original
        Instances r = new Instances(orig);
        
        // names of target attributes to square.
        // square only the times for distance of travel between keys
        String[] A = {
            "updown1",  "updown2",  "updown3", 
            "updown4",  "updown5",  "updown6", 
            "updown7",  "updown8",  "updown9", 
            "updown10", "updown11", "updown12",
            "updown13"
        };
        
        for (Enumeration<Instance> e = r.enumerateInstances(); e.hasMoreElements();){
            Instance d = e.nextElement();
            for (int i = 0; i < A.length; ++i){
                Attribute a = r.attribute(A[i]);
                d.setValue(a,d.value(a)*d.value(a));
            }
        }
        return r;
    }

    public static Instances addTotalTime(Instances orig){
        // create return object wih modified data of original
        Instances r = new Instances(orig);
        r.insertAttributeAt(new Attribute("totaltime"),r.numAttributes()-1);

        // Sum of these values will equate to total time 
        String[] A = {
            "holdtime1",  "holdtime2",  "holdtime3", 
            "holdtime4",  "holdtime5",  "holdtime6", 
            "holdtime7",  "holdtime8",  "holdtime9", 
            "holdtime10", "holdtime11", "holdtime12",
            "holdtime13", "holdtime14", "updown1",   
            "updown2",    "updown3",    "updown4",   
            "updown5",    "updown6",    "updown7",   
            "updown8",    "updown9",    "updown10",  
            "updown11",   "updown12",   "updown13"
        };
        
        for (Enumeration<Instance> e = r.enumerateInstances(); e.hasMoreElements();){
            Instance d = e.nextElement();
            int tt = 0;
            for (int i = 0; i < A.length; ++i){
                tt+=d.value(r.attribute(A[i]));
            }
            d.setValue(r.attribute("totaltime"),tt);
        }
        return r;
    }

    public static void runExperiment(Classifier classifier, Instances data){
        ArrayList<Double> EER = new ArrayList<Double>();
        double EER_MEAN = 0; // mean of EER values
        double EER_SD  = 0; // standard deviation of EER values
        try {
            File f = File.createTempFile("rcallertmp",".tmp");
            FileWriter writer;
            PrintWriter pwriter;
            RCaller caller;
            RCode code;
            
            data.setClassIndex(data.numAttributes()-1);
            Evaluation eval = new Evaluation(data);
            
            eval.crossValidateModel(classifier, data, 10, new Random(1));
            ThresholdCurve tc = new ThresholdCurve();
            Attribute labels = data.attribute(data.numAttributes()-1);
            for (int i = 0; i < labels.numValues(); ++i){
                Instances result = tc.getCurve(eval.predictions(), i);
                writer = new FileWriter(f);
                pwriter = new PrintWriter(writer);
                pwriter.println(result);
                pwriter.flush();
                pwriter.close();

                // R code
                caller = RCaller.create();
                code = RCode.create();
                code.addRCode("library(foreign)");
                code.addRCode("arff = read.arff(\"" + f.getAbsoluteFile() +"\")");
                code.addRCode("arff[,\"False Negative Rate\"] " +
                              "<- sapply(arff[,c(\"True Positive Rate\")], " +
                              "function(x) 1 - x)");
                code.addRCode("data = arff[,c(\"False Negative Rate\"," +
                              "\"False Positive Rate\",\"Threshold\")]");
                code.addRCode("data = rbind(data,c(1,0,1))");
                code.addRCode("f = approxfun(x=data[,c(\"Threshold\")]," +
                              "y=data[,c(\"False Negative Rate\")])");
                code.addRCode("g = approxfun(x=data[,c(\"Threshold\")]," +
                              "y=data[,c(\"False Positive Rate\")])");
                code.addRCode("res = uniroot(function(x) f(x)-g(x),interval = c(0,1))");
                code.addRCode("eer = f(res[\"root\"])");                
                caller.setRCode(code);
                caller.runAndReturnResult("eer");
                
                double[] eer = caller.getParser().getAsDoubleArray("eer");
                // R code end
                EER.add(eer[0]);
                EER_MEAN+=eer[0];
            }
            EER_MEAN/=EER.size();
            for (int i = 0; i < EER.size(); ++i)
                EER_SD+=(EER.get(i)-EER_MEAN)*(EER.get(i)-EER_MEAN);
            EER_SD/=EER.size();
            EER_SD = Math.sqrt(EER_SD);
            
            System.out.println(eval.toSummaryString("\nResults\n\n", false));
            System.out.println("Mean EER\t\t: " + EER_MEAN);
            System.out.println("Standard Deviation\t: " + EER_SD);            
            
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    public static void main(String[] args){
        try {
        Instances data = DataSource.read("./analysis/keystroke_71features.arff");
        // System.out.println("**********************************************");
        // System.out.println("RandomForest (10 fold cv) w/original data set");        
        // runExperiment(new RandomForest(),data);
        // System.out.println("**********************************************");
        // System.out.println("RandomForest (10 fold cv) w/ square distance data set");
        // runExperiment(new RandomForest(),squareValues(data));
        // System.out.println("**********************************************");
        // System.out.println("RandomForest (10 fold cv) w/ total time attribute data set");
        // runExperiment(new RandomForest(),addTotalTime(data));
        // System.out.println("**********************************************");        
        // System.out.println("RandomForest (10 fold cv) w/ total time && squared data set");
        // runExperiment(new RandomForest(),squareValues(addTotalTime(data)));

        // System.out.println("**********************************************");
        // System.out.println("BayesNet (10 fold cv) w/original data set");        
        // runExperiment(new BayesNet(),data);
        // System.out.println("**********************************************");
        // System.out.println("BayesNet (10 fold cv) w/ square distance data set");        
        // runExperiment(new BayesNet(),squareValues(data));
        // System.out.println("**********************************************");
        // System.out.println("BayesNet (10 fold cv) w/ total time attribute data set");
        // runExperiment(new BayesNet(),addTotalTime(data));
        // System.out.println("**********************************************");        
        // System.out.println("BayesNet (10 fold cv) w/ total time && squared data set");
        // runExperiment(new BayesNet(),squareValues(addTotalTime(data)));

        // System.out.println("**********************************************");
        // System.out.println("NaiveBayes (10 fold cv) w/original data set");        
        // runExperiment(new NaiveBayes(),data);
        // System.out.println("**********************************************");
        // System.out.println("NaiveBayes (10 fold cv) w/ filtered dataset");        
        // runExperiment(new NaiveBayes(),filter_NB(data));
        // System.out.println("**********************************************");        

        // Int[] indices = {0,1,2,3,4,5,6,9,10,
        //                  11,12,15,22,24,28,34
        //                  ,38,40,42,45,46,47,48
        //                  ,51,52,53,54,55,58,59,
        //                  60,61,63,65,66,68,69,70,71};
        // System.out.println("Redundant attributes: ");
        // for (int i = 0; i < data.numAttributes(); ++i){
        //     Boolean print = true;
        //     for (int j = 0; j < indices.length; ++j){
        //         if (indices[j] == i){
        //             print = false;
        //             break;
        //         }
        //     }
        //     if (print){
        //         System.out.println(data.attribute(i));
        //     }
        // }
            
        System.out.println("**********************************************");
        System.out.println("RandomForest (10 fold cv) w/original data set");        
        runExperiment(new RandomForest(),data);
        System.out.println("**********************************************");
        System.out.println("RandomForest (10 fold cv) w/ filtered dataset");        
        runExperiment(new RandomForest(),filter_BNet(data));
        System.out.println("**********************************************");        

        int[] indices = {0,1,2,3,4,5,6,8,9,10,11,12,13,15,16,17,18,20,21,22,24,25,26,27,28,29,31,32,33,34,35,36,37,39,40,42,44,45,46,48,50,51,53,54,55,56,57,58,59,60,61,62,63,64,65,67,68,69,70,71};
        System.out.println("Redundant attributes: ");
        for (int i = 0; i < data.numAttributes(); ++i){
            Boolean print = true;
            for (int j = 0; j < indices.length; ++j){
                if (indices[j] == i){
                    print = false;
                    break;
                }
            }
            if (print){
                System.out.println(data.attribute(i));
            }
        }
            
            
        
        } catch (Exception ex){
            System.out.println(ex.getMessage());
        }
    }
}
