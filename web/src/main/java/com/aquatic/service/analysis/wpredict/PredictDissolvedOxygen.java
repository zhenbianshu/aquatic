package com.aquatic.service.analysis.wpredict;

import com.aquatic.service.preprocessing.entity.Parameters;
import com.aquatic.service.preprocessing.process.Normalization;
import com.aquatic.service.preprocessing.process.Preprocessing;
import com.aquatic.service.preprocessing.process.TrainTestSplit;
import com.aquatic.service.preprocessing.utils.DataUtils;
import com.aquatic.utils.PathHelper;
import water_para_predict.WaterQualityPredict;
import water_quality_svr_train.WaterQualitySVMTrain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PredictDissolvedOxygen {
    public static void main(String[] args){
        String path = "E:/prediction/fiveparam1/cut";
        PredictUtils tp = new PredictUtils();
        PredictDissolvedOxygen pdo = new PredictDissolvedOxygen();
        Map<String, List<List<Parameters>>> setMap;
        try {
            //String filePath,int centerNumber,int para, int position
            //文件位置、聚类中心个数、预测参数位置、错开位数
            long start = System.currentTimeMillis();
            setMap = TrainTestSplit.train_test_split(path,3,1,1);
            List<Parameters> testSet = tp.getTestSet(setMap,3,"testSet");
            //预测参数位置
            List<Double> targetFactor =tp.getTargetFactor(testSet, 12);
            List<Double> result = new ArrayList<>();
            for(int i=0;i<3;i++){
                List<Double> predict = pdo.predictDissolvedOxygen(i,setMap);
                result.addAll(predict);
            }
            long end = System.currentTimeMillis();
            double time = (end-start)/1000;
            System.out.println(time);
            double[] normalizedArray = DataUtils.listToArray(result);
            List<Parameters> entityList1 = Preprocessing.getEntityList("E:/prediction/atmosphere.csv");
            //水质数据预处理
            List<Parameters> entityList2 = Preprocessing.preprocessWater();
            //数据融合，得到原始数据集，即可还原序列
            List<Parameters> fusedList = Preprocessing.dataFusion(entityList1,entityList2);
            List<Double> inversedList = Normalization.inverse(fusedList, normalizedArray, 1);
            double[] targetArray = DataUtils.listToArray(targetFactor);
            List<Double> inversedTarget = Normalization.inverse(fusedList, targetArray, 1);
            double mae = DataUtils.getMAE(inversedTarget, inversedList);
            double mape = DataUtils.getMAPE(inversedTarget, inversedList);
            double rmse = DataUtils.getRMSE(inversedTarget,inversedList);
            System.out.println(mae);
            System.out.println(mape);
            System.out.println(rmse);
            for(int i=0;i<inversedList.size();i++){
                System.out.println(inversedList.get(i)+"-"+inversedTarget.get(i));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public List<Double> predictDissolvedOxygen(int setSerialNumber,Map<String,List<List<Parameters>>> setMap){
        List<Double> result = new ArrayList<>();
        double[] resultArray = null;
        try {
            double[][] trainingSet = this.getPredictFators(setMap,setSerialNumber,"trainingSet");
            double[][] testSet = this.getPredictFators(setMap,setSerialNumber,"testSet");

            WaterQualitySVMTrain wqsvmt = new WaterQualitySVMTrain();

            Object[] originResult = wqsvmt.water_para_train(3, trainingSet);
            System.out.println("=========================寻参完成==============================");

            double[][] train = this.reConstructTrainingSet(testSet, trainingSet, originResult);
            WaterQualityPredict wqp = new WaterQualityPredict();
            Object[] predict = wqp.water_para_model(1, train);
            //System.out.println(predict[0].toString());
            resultArray = PredictUtils.result2array(predict[0].toString());
            result = DataUtils.arrayToList(resultArray);
        }catch (Exception e){
            System.out.println("--------------------------------------------");
            System.out.println("预测出现问题！");
            e.printStackTrace();
        }
        return result;
    }

    private double[][] reConstructTrainingSet(double[][] testSet,double[][] trainingSet,Object[] originResult){
        double[] cgp = new double[originResult.length];
        //参数
        for(int i=0;i<originResult.length;i++){
            cgp[i] = Double.parseDouble(originResult[i].toString().trim());
        }
        int rows= testSet.length+trainingSet.length;
        double[][] train = new double[rows+1][testSet[0].length];
        for(int i=0;i<testSet.length;i++){
            train[i] = trainingSet[i];
        }
        for(int i=testSet.length;i<rows;i++){
            train[i] = trainingSet[i-testSet.length];
        }
        for(int i=0;i<originResult.length;i++){
            train[rows][i] = cgp[i];
        }
        train[rows][3]= testSet.length;
        return train;
    }


    //注意，这个地方行列都需要注意，加上标签，最后一列是标签
    private double[][] getPredictFators(Map<String,List<List<Parameters>>> setMap,int setSerialNumber,String type){
        double[][] result = null;
        List<List<Parameters>> trainSets = setMap.get(type);
        List<Parameters> trainingSet = trainSets.get(setSerialNumber);
        //溶解氧的预测使用 溶解氧 水温 风速 大气温度 大气压力 太阳辐射 相对湿度 作为预测因子
        //以上预测因子所对应序号为0、1、5、6、7、8、9、10
        int row = trainingSet.size();
        int col = 8;
        result = new double[row][col];
        for(int i=0;i<row;i++){
            Parameters parameter = trainingSet.get(i);
            result[i][0] = parameter.getParaList().get(0);//水温
            result[i][1] = parameter.getParaList().get(1);//溶解氧
            result[i][2] = parameter.getParaList().get(5);//大气湿度
            result[i][3] = parameter.getParaList().get(6);//大气温度
            result[i][4] = parameter.getParaList().get(7);//大气压力
            //result[i][5] = parameter.getParaList().get(8);//风速
            //result[i][6] = parameter.getParaList().get(9);//风向
            result[i][5] = parameter.getParaList().get(2);//pH
            result[i][6] = parameter.getParaList().get(3);//浊度
            //result[i][7] = parameter.getParaList().get(10);//太阳辐射
            result[i][7] = parameter.getParaList().get(12);//标签
        }
        return result;
    }
}
