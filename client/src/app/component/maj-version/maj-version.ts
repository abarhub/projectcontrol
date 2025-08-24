import {Component, ElementRef, inject, OnDestroy, ViewChild} from '@angular/core';
import {Modal} from 'bootstrap';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {KeyValuePipe} from '@angular/common';
import {MajVersionService} from '../../service/maj-version.service';
import {Subscription} from 'rxjs';
import {MajVersionApi} from '../../entity/maj-version-api';
import {ToasterService} from '../../service/toaster.service';

@Component({
  selector: 'app-maj-version',
  imports: [
    ReactiveFormsModule,
    KeyValuePipe
  ],
  templateUrl: './maj-version.html',
  styleUrl: './maj-version.scss'
})
export class MajVersion implements OnDestroy {

  @ViewChild('toastEl', {static: true}) majVersionEl!: ElementRef;

  private majVersionModal!: Modal;

  myForm: FormGroup;

  versions: Map<string, string> = new Map<string, string>();
  versionActuelle: string = "";
  messageCommitTemplate: string = "";
  versionAutre: string = "-1";
  private groupeId: string = "";
  private nomProjet: string = "";
  private toasterService = inject(ToasterService);

  changes: Subscription | undefined;
  changes2: Subscription | undefined;
  changes3: Subscription | undefined;

  constructor(private fb: FormBuilder, private majVersionService: MajVersionService) {
    this.myForm = this.fb.group({
      choixVersion: ['', Validators.required],
      versionAutre: [{value: '', disabled: true}],
      commitActive: ['', Validators.required],
      commitMessage: ['', Validators.required]
    });

    this.changes = this.myForm.get('choixVersion')?.valueChanges
      .subscribe(changes => {
        console.log('choixVersion', changes);
        if (changes && changes == this.versionAutre) {
          this.myForm.controls['versionAutre'].enable();
        } else {
          this.myForm.controls['versionAutre'].disable();
        }
        this.calculMessageCommit(changes);
      });

    this.changes2 = this.changes = this.myForm.get('commitActive')?.valueChanges
      .subscribe(changes => {
          console.log('commitActive', changes);
          if (changes) {
            this.myForm.controls['commitMessage'].enable();
          } else {
            this.myForm.controls['commitMessage'].disable();
          }
        }
      );

    this.changes3 = this.changes = this.myForm.get('versionAutre')?.valueChanges
      .subscribe(changes => {
          console.log('versionAutre', changes);
          this.calculMessageCommit(changes);
        }
      );
  }

  ngOnDestroy() {
    this.changes?.unsubscribe();
    this.changes2?.unsubscribe();
    this.changes3?.unsubscribe();
  }

  private calculMessageCommit(version: string) {
    if (this.messageCommitTemplate && version) {
      let versionSelectionne = this.getVersion(version);
      if (versionSelectionne) {
        let message = this.messageCommitTemplate.replace("VERSION", versionSelectionne);
        this.myForm.controls['commitMessage'].setValue(message);
      }

    }
  }

  public show(groupeId: string, nomProjet: string) {
    this.groupeId = groupeId;
    this.nomProjet = nomProjet;

    if (this.majVersionEl) {
      this.majVersionModal = new Modal(this.majVersionEl.nativeElement);
      if (this.majVersionModal) {

        this.majVersionService.getVersions(groupeId, nomProjet).subscribe({
            next: (data) => {

              console.info("affichage", data);
              if (data) {
                if (data.versionActuelle) {
                  this.messageCommitTemplate = data.messageCommit;
                  // this.myForm.controls['commitMessage'].setValue(data.messageCommit);
                  //this.versions.set(data.versionActuelle, data.versionActuelle);
                  this.versionActuelle = data.versionActuelle;
                  if (data.listeVersions && data.listeVersions.length > 0) {
                    for (let i = 0; i < data.listeVersions.length; i++) {
                      let version = data.listeVersions[i];
                      this.versions.set("" + (i + 1), version);
                    }
                  }

                  this.myForm.setValue({
                    choixVersion: '',
                    versionAutre: '',
                    commitActive: true,
                    commitMessage: ""
                  });

                  this.majVersionModal.show();
                }
              }


            }, error: (error) => {
              console.error(error);
              this.toasterService.show("Erreur pour charger les versions");
            }
          }
        );


      }
    }
  }

  enregistrer($event: MouseEvent) {
    $event.preventDefault();
    let majVersion: MajVersionApi = new MajVersionApi();
    let version = this.getVersion(this.myForm.controls['choixVersion'].value);
    if (version) {
      majVersion.version = version;
      majVersion.commit = !!this.myForm.controls['commitActive'].value;
      if (majVersion.commit) {
        majVersion.messageCommit = this.myForm.controls['commitMessage'].value;
      }
      this.majVersionService.majVersions(this.groupeId, this.nomProjet, majVersion)
        .subscribe({
          next: (data) => {
            console.info("majVersion", data);
            this.toasterService.show("majVersion OK");
            this.majVersionModal.hide();
          },
          error: (error) => {
            console.error(error);
            this.toasterService.show("Erreur pour majVersion");
          }
        });
    }
  }

  private getVersion(changes: string): string {
    if (changes) {
      let versionSelectionne = '';
      if (changes === this.versionAutre) {
        let versionAutre2 = this.myForm.controls['versionAutre'].value;
        if (versionAutre2) {
          versionSelectionne = versionAutre2;
        } else {
          versionSelectionne = "";
        }
      } else {
        versionSelectionne = changes;
      }
      return versionSelectionne;
    }
    return "";
  }
}
