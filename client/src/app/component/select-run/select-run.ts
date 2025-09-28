import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {Modal} from 'bootstrap';
import {ToasterService} from '../../service/toaster.service';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {RunConfig} from '../../entity/run-config';
import {Observable, ReplaySubject, Subject} from 'rxjs';

@Component({
  selector: 'app-select-run',
  imports: [
    ReactiveFormsModule
  ],
  templateUrl: './select-run.html',
  styleUrl: './select-run.scss'
})
export class SelectRun {

  @ViewChild('toastEl', {static: true}) majVersionEl!: ElementRef;

  private majVersionModal!: Modal;


  private groupeId: string = "";
  private nomProjet: string = "";
  private toasterService = inject(ToasterService);
  listeRunConfig: RunConfig[] = [];
  private subject: Subject<string> = new ReplaySubject<string>();


  formGrouId3 = new FormGroup({
    recherche: new FormControl(''),
  });

  public show(groupeId: string, nomProjet: string, listeRunConfig: RunConfig[]): Observable<string> {
    this.groupeId = groupeId;
    this.nomProjet = nomProjet;
    this.listeRunConfig = listeRunConfig;
    let obs = this.createObservable();
    this.subject = obs[0];
    this.majVersionModal = new Modal(this.majVersionEl.nativeElement);
    this.majVersionModal.show();
    return obs[1];
  }


  executer($event: MouseEvent) {
    $event?.preventDefault();

    let select = this.formGrouId3.value.recherche;
    if (select) {
      this.subject.next(select);
      this.subject.complete();
      this.majVersionModal.hide();
    }

  }

  private createObservable(): [Subject<string>, Observable<string>] {
    const subj = new ReplaySubject<string>(1);
    return [subj, subj.asObservable()];
  }

}
