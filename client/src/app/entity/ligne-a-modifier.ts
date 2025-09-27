import {Interval} from './interval';

export class LigneAModifier {
  ligne: number = 0;
  contenu: string = '';
  trouve: boolean = false;
  positionModification: Interval[] = [];
  nomForm: string = '';
  id: string = '';
  lignesDecoupees: string[] = [];
  debutTrouve: boolean = false;
}
