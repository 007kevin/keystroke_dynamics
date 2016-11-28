import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.trees.*;
import weka.classifiers.bayes.*;

import java.util.Random;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import com.github.rcaller.rstuff.*;

public class EqualError {
    public static void main(String[] args){
        double MEAN_EER = 0;
        try {
            File f = File.createTempFile("rcallertmp",".tmp");
            FileWriter writer;
            PrintWriter pwriter;
            RCaller caller;
            RCode code;
            
            // Instances data = DataSource.read("./analysis/keystroke_71features.arff");
            Instances data = DataSource.read("logicalstrong_secondorder.arff");
            
            data.setClassIndex(data.numAttributes()-1);
            Evaluation eval = new Evaluation(data);
            BayesNet classifier = new BayesNet();
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
                //                code.addRCode("data = rbind(data,c(1,0,1))");
                code.addRCode("f = splinefun(x=data[,c(\"Threshold\")]," +
                              "y=data[,c(\"False Negative Rate\")])");
                code.addRCode("g = splinefun(x=data[,c(\"Threshold\")]," +
                              "y=data[,c(\"False Positive Rate\")])");
                code.addRCode("res = uniroot(function(x) f(x)-g(x),interval = c(0,1))");
                code.addRCode("eer = f(res[\"root\"])");                
                caller.setRCode(code);
                caller.runAndReturnResult("eer");
                
                double[] eer = caller.getParser().getAsDoubleArray("eer");
                // R code end
                
                //System.out.println(eer[0]);
                MEAN_EER+=eer[0];
            }
            System.out.println("Mean EER: " + MEAN_EER/labels.numValues());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
