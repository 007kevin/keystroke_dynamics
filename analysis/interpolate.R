library("foreign")
arff = read.arff("keystroke_71features.threshold.arff")
arff[,"False Negative Rate"] <- sapply(arff[,c("True Positive Rate")], function(x) 1 - x)
data = arff[,c("False Negative Rate","False Positive Rate","Threshold")]
data = rbind(data,c(1,0,1))
f = approxfun(x=data[,c("Threshold")],y=data[,c("False Negative Rate")])
g = approxfun(x=data[,c("Threshold")],y=data[,c("False Positive Rate")])
graph <- function(){
    curve(f,from=0,to=1,n=101)
    curve(g,from=0,to=1,n=101,add=TRUE)
}

eer <- function(){
    res = uniroot(function(x) f(x)-g(x),interval = c(0,1))
    f(res["root"])  
}
