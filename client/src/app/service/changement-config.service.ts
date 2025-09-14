import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ChangementConfigApi} from '../entity/changement-config-api';

@Injectable({
  providedIn: 'root'
})
export class ChangementConfigService {
  private http = inject(HttpClient);
  private baseUrl = '/api/changement-config';


  getChangementConfig(groupeId: string, nom: string, comminInitial: string, commitFinal: string): Observable<ChangementConfigApi> {
    return this.http.get<ChangementConfigApi>(this.baseUrl + '/' + groupeId + '/' + nom+'?commitInitial='+comminInitial+'&commitFinal='+commitFinal);
  }
}
