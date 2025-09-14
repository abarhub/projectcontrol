import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ReponseRechercheInitial} from '../entity/reponse-recherche-initial';
import {ReponseRechercheSuivante} from '../entity/reponse-recherche-suivante';

@Injectable({
  providedIn: 'root'
})
export class RechercheService {


  private http = inject(HttpClient);
  private baseUrl = '/api/recherche';

  getRecherche(groupeId: string, texte: string, typeRecherche: string,
               projetId: string,  nbLignesAutour:number): Observable<ReponseRechercheInitial> {
    return this.http.get<ReponseRechercheInitial>(this.baseUrl + '/' + groupeId + "?texte=" + encodeURIComponent(texte) +
        "&typeRecherche=" + encodeURIComponent(typeRecherche) +
      ((projetId) ? "&projetId=" + encodeURIComponent(typeRecherche) : "")+
      ((nbLignesAutour>0) ? "&nbLignesAutour=" + nbLignesAutour : ""));
  }

  getRechercheSuivant(id: string): Observable<ReponseRechercheSuivante> {
    return this.http.get<ReponseRechercheSuivante>(this.baseUrl + '/suite/' + id);
  }
}
