#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int counter;
float pagefaults;
float pagefaultrate;

char data[256];
char *physicalMemory;
FILE *f;
FILE *f2;
int i,currentInt,page_number,page_offset;
void InttoBinary(int input);
int pageNumToInt();
int pageOffsetToInt();

char binaryInt[33];
char pageNum[9];
char pageOffset[9];

struct PageTable{
	int numEntries;
	int physical[256];
};
struct PageTable p_table;

int main(int argc, char **argv){
	counter = 0;
	physicalMemory = malloc(256*256);
	for(i = 0; i < 256;i++){
		p_table.physical[i] = -1;
	}
	p_table.numEntries = 0;

	f = fopen(argv[2],"r");
	if(f == 0){
		printf("ERROR OPENING FILE!\n");
		exit(1);
	}	
	while(fscanf(f,"%s",data) != EOF){
	currentInt = atoi(data);
	printf("%d - Virtual Address %d - ",counter,currentInt);
	InttoBinary(currentInt);

	//printf("Binary:%s\n",binaryInt);
	for(i = 16; i < 24;i++){
		pageNum[i-16] = binaryInt[i]; 
	}
	pageNum[8] = '\0';
	for(i = 24; i < 32; i++){
		pageOffset[i-24] = binaryInt[i];
	}
	pageOffset[8] = '\0';
	//printf("PAGE NUM: %s\n",pageNum);
	int page_number = pageNumToInt();
	//printf("In decimal: %d\n",page_number);
	//printf("PAGE OFFSET: %s\n",pageOffset);
	int page_offset = pageOffsetToInt();
	//printf("In decimal: %d\n",page_offset);

	if(p_table.physical[page_number] == -1){
		//printf("PAGE FAULT\n");
		pagefaults++;
		f2 = fopen(argv[1],"rb");
		if(f2 == 0){
			printf("ERROR OPENING BACKING STORE!\n");
			exit(1);
		}
		fseek(f2,page_number*256, SEEK_SET);
		fread(&physicalMemory[p_table.numEntries*256],256,1,f2);

		p_table.physical[page_number] = p_table.numEntries*256;

		printf(" Physical address %d - Value  %d\n",p_table.physical[page_number]+page_offset,physicalMemory[p_table.physical[page_number]+page_offset]);	
		p_table.numEntries++;

		fclose(f2);		
	}else{
		printf(" *HIT* ");
		printf(" Physical address %d - Value %d\n",p_table.physical[page_number]+page_offset,physicalMemory[p_table.physical[page_number]+page_offset]);
	}
	counter++;
}
printf("Number of translated adresses: %d\n",counter);
printf("Page fault total: %f\n",pagefaults);
printf("Page fault rate: %f\n",pagefaults/counter);

fclose(f);
free(physicalMemory);
return 0;
}

int pageNumToInt(){
	char *end = NULL;
	return strtoul(pageNum,&end,2);
}
int pageOffsetToInt(){
	char *end = NULL;
	return strtoul(pageOffset,&end,2);
}
void InttoBinary(int current){
	int c,k;
	for(c = 31; c>=-0;c--){
		k= current>>c;
		if(k&1){
			binaryInt[31-c]='1';
		}else{
			binaryInt[31-c]='0';
		}
	}
	binaryInt[32] = '\0';
	//printf("%s\n",binaryInt);
}
