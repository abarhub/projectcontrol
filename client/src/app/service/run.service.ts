import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ReponseRunInitial} from '../entity/reponse-run-initial';
import {ReponseRunSuivante} from '../entity/reponse-run-suivante';
import {RunConfig} from '../entity/run-config';

@Injectable({
  providedIn: 'root'
})
export class RunService {

  private http = inject(HttpClient);
  private baseUrl = '/api/run';

  getRun(groupeId: string, nom: string, action: string): Observable<ReponseRunInitial> {
    return this.http.get<ReponseRunInitial>(this.baseUrl + '/' + groupeId + '/' + nom + "?action=" + encodeURIComponent(action));
  }

  getRunSuivant(id: string): Observable<ReponseRunSuivante> {
    return this.http.get<ReponseRunSuivante>(this.baseUrl + '/suite/' + id);
  }

  getListConfig(): Observable<RunConfig[]> {
    return this.http.get<RunConfig[]>(this.baseUrl + '/liste-run-config');
  }
}
