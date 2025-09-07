import {Interval} from './interval';

export class LigneGrep {

  noLigne: number = 0;
  ligne: string = '';
  trouve: boolean = false;
  range: Interval[] = [];
  lignesDecoupees: string[] = [];
  debutTrouve: boolean = false;
}
