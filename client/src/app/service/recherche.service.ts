import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {LigneResultat} from '../entity/LigneResultat';

@Injectable({
  providedIn: 'root'
})
export class RechercheService {


  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/recherche';

  getRecherche(groupeId: string, texte: string): Observable<LigneResultat[]> {
    return this.http.get<LigneResultat[]>(this.baseUrl + '/' + groupeId + "?texte=" + texte);
  }
}
