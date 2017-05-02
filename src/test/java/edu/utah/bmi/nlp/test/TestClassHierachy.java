package edu.utah.bmi.nlp.test;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by
 *
 * @author Jianlin Shi on 5/1/17.
 */
public class TestClassHierachy {
    @Test
    public void test(){
        List<String> list=new ArrayList<>();
        ArrayList<String> list2 = (ArrayList<String>) list;

    }
}
