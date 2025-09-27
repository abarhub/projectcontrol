import {Injectable} from '@angular/core';
import {Interval} from '../entity/interval';

@Injectable({
  providedIn: 'root'
})
export class StringService {

  public decoupeLigne(ligne: string, range: Interval[]): [string[], boolean] {
    let liste: string[] = [];
    let debutRouge = false;
    let pos = 0;
    for (let i = 0; i < range.length; i++) {
      let range2 = range[i];
      if (range2.debut == pos) {
        let s = ligne.substring(pos, range2.fin + 1);
        liste.push(s);
        pos = range2.fin + 1;
        if (i == 0) {
          debutRouge = true;
        }
      } else {
        let s = ligne.substring(pos, range2.debut - 1 + 1);
        liste.push(s);
        s = ligne.substring(range2.debut, range2.fin + 1);
        liste.push(s);
        pos = range2.fin + 1;
        if (i == 0) {
          debutRouge = false;
        }
      }
    }
    if (pos < ligne.length) {
      let s = ligne.substring(pos, ligne.length);
      liste.push(s);
    }
    return [liste, debutRouge];
  }

}
