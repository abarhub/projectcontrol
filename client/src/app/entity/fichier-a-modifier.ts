import {LigneAModifier} from './ligne-a-modifier';

export class FichierAModifier {
  nomFichier: string = '';
  hash: string = '';
  lignes: Map<number, LigneAModifier> = new Map();

}
