/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class quickSort <T extends Number> {

private int first, last, boundLo, boundHi, pivot;
int temp[] = {0, 0};

public quickSort() {
    super();
}

int [] newOrder;

public int [] getNewORDEM(double [] fitness){
newOrder = new int[fitness.length];
int [] reverse = new int[fitness.length];

List<Double> list = new ArrayList<>();
for(int f=0;f<fitness.length;f++){
    list.add(fitness[f]);
    newOrder[f] = f;
}

sort(list);

for(int f=0;f<fitness.length;f++){
reverse[f] = newOrder[fitness.length-f-1];
}

return reverse;
}


public void sort(List<Double> list) {

    Deque<int[]> stack = new ArrayDeque<>();

    first = 0;
    last = list.size() - 1;

    stack.push(new int[] {first, last});

    while(!stack.isEmpty()) {
        sortStep(list, stack);  
    }
}

private void troca(int n1, int n2){
int i;
i = newOrder[n1];
newOrder[n1] = newOrder[n2];
newOrder[n2] = i;
}


private void sortStep(List<Double> list, Deque<int[]> stack) {

    temp = stack.pop(); //criando stack local
    first = temp[0];
    last = temp[1];

    boundLo = first;
    boundHi = last;
    pivot = last;

    while(first < last) {
        if(list.get(first).doubleValue() >= list.get(pivot).doubleValue()) {
            last--;
            if(first != last){ 
                Collections.swap(list, first, last);         //ordenação   
                troca(first,last);
            }
            Collections.swap(list, last, pivot);   //ordenação
            troca(last,pivot);
            
            pivot--;
        }
        else first++;
    }

    if(boundLo < (pivot - 1)) 
        stack.add(new int[] {boundLo, pivot - 1});

    if(boundHi > (pivot + 1)) 
        stack.add(new int[] {pivot + 1, boundHi});
}

}