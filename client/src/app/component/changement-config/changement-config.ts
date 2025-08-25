import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {Modal} from 'bootstrap';
import {MajVersionService} from '../../service/maj-version.service';
import {ToasterService} from '../../service/toaster.service';
import {ChangementConfigService} from '../../service/changement-config.service';

@Component({
  selector: 'app-changement-config',
  imports: [
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './changement-config.html',
  styleUrl: './changement-config.scss'
})
export class ChangementConfig {

  @ViewChild('toastEl', {static: true}) majVersionEl!: ElementRef;

  private majVersionModal!: Modal;

  myForm: FormGroup;

  private groupeId: string = "";
  private nomProjet: string = "";
  private toasterService = inject(ToasterService);
  messages: string [] = [];
  private changementConfigService = inject(ChangementConfigService);


  constructor(private fb: FormBuilder, private majVersionService: MajVersionService) {
    this.myForm = this.fb.group({
      commitInitial: ['', Validators.required],
      commitFinal: ['', Validators.required]
    });
  }

  public show(groupeId: string, nomProjet: string) {
    this.groupeId = groupeId;
    this.nomProjet = nomProjet;
    this.majVersionModal = new Modal(this.majVersionEl.nativeElement);
    this.majVersionModal.show();
  }

  enregistrer($event: MouseEvent) {
    let commitInitial = this.myForm.controls['commitInitial'].value;
    let commitFinal = this.myForm.controls['commitFinal'].value;
    if (commitInitial && commitFinal) {
      this.changementConfigService.getChangementConfig(this.groupeId, this.nomProjet, commitInitial, commitFinal).subscribe({
        next: (data) => {
          console.info("change config", data);
          this.toasterService.show("change config OK");
          if (data.resultat) {
            this.messages = data.resultat;
          }
        },
        error: (error) => {
          console.error(error);
          this.toasterService.show("Erreur pour change config");
        }
      })
    }
  }
}
