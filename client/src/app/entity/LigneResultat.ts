import {LigneGrep} from './ligne-grep';

export class LigneResultat {
  noLigne: number = 0;
  ligne: string = '';
  lignes: string[] = [];
  lignes2: LigneGrep[] = [];
  fichier: string = '';
  repertoireParent: string = '';
  trouve: boolean = false;
  listeLigneGrep: LigneGrep[]|null = null;
}
