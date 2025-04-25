import {Component, Inject} from '@angular/core';
import {
    MAT_DIALOG_DATA,
    MatDialogActions,
    MatDialogContent,
    MatDialogRef,
    MatDialogTitle
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';
import {FormsModule} from '@angular/forms';
import {MatFormField} from '@angular/material/form-field';
import {MatInput} from '@angular/material/input';


@Component({
    selector: 'app-routes-dialog',
    imports: [
        MatDialogTitle,
        MatButton,
        MatDialogActions,
        MatDialogContent,
        FormsModule,
        MatFormField,
        MatInput
    ],
    templateUrl: './routes-dialog.component.html',
    styleUrl: './routes-dialog.component.css'
})
export class RoutesDialogComponent {

    population: number = 1;

    constructor(
        public dialogRef: MatDialogRef<RoutesDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: { population: number }
    ) {
    }

    onCancel() {
        this.dialogRef.close(false)
    }

    onConfirm() {
        this.dialogRef.close(this.population)
    }
}
