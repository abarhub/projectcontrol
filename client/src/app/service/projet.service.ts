import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Projet} from '../entity/projet';
import {GroupeProjet} from '../entity/groupe-projet';

@Injectable({
  providedIn: 'root'
})
export class ProjetService {
  private http = inject(HttpClient);
  private baseUrl = '/api/projet';

  getProjets(groupeId: string): Observable<Projet[]> {
    return this.http.get<Projet[]>(this.baseUrl + '/from-groupId/' + groupeId);
  }

  getProjet(groupeId: string, nom: string): Observable<Projet> {
    return this.http.get<Projet>(this.baseUrl + '/' + groupeId + '/' + nom);
  }

  getGroupeProjet(): Observable<GroupeProjet> {
    return this.http.get<GroupeProjet>(this.baseUrl + '/groupe-projets');
  }
}
