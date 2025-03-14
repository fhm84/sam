import { Instrumentation } from "./instrumentation";

export interface SheetMusic {
     
     id: string;
     /**
     * The title of the music sheetMusic
     */
     title: string;
     /**
      * The publisher of the music sheetMusic
      */
     publisher: string;
     /**
      * The composer of the music sheetMusic
      */
     composer: string;
     type: string;
     miscellaneous: string;
     rating: number;

     genre: string;

     instrumentations: Instrumentation[];
}
