import {LigneResultat} from './LigneResultat';

export class ReponseRechercheSuivante {
  id: string = '';
  listeLignes:LigneResultat[]=[];
  terminer: boolean = false;
}
