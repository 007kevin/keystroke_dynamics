import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.trees.*;
import weka.classifiers.bayes.*;
import java.util.Random;
import java.util.Enumeration;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import com.github.rcaller.rstuff.*;

public class EqualError {
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
    
    public static void main(String[] args){
        double MEAN_EER = 0;
        try {
            File f = File.createTempFile("rcallertmp",".tmp");
            FileWriter writer;
            PrintWriter pwriter;
            RCaller caller;
            RCode code;
            
             // Instances data = DataSource.read("./analysis/keystroke_71features.arff");
            Instances orig = DataSource.read("analysis/keystroke_71features.arff");
            // Instances data = squareValues(orig);
            // Instances data = addTotalTime(orig);
            Instances data = squareValues(addTotalTime(orig));
            
            data.setClassIndex(data.numAttributes()-1);
            Evaluation eval = new Evaluation(data);
            RandomForest classifier = new RandomForest();
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
                
                MEAN_EER+=eer[0];
            }
            System.out.println(eval.toSummaryString("\nResults\n\n", false));
            System.out.println("Mean EER: " + MEAN_EER/labels.numValues());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}