package com.stages

import com.tools.Tool

class Stage implements Serializable {

   private LinkedList tools

    Stage() {
      this.tools = [] as LinkedList
 }

 void add(Tool tool){
   this.tools.add(tool)

  }


 void execute(steps){
    for(int i=0;i< this.tools.size();i++) {
      this.tools.get(i).execute(steps)
   }
 }
}