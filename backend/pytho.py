import csv

mapaPeso = []
semana = {}
try:
    with open ('comprovante.csv', 'r', encoding='utf-8') as csvfile:
        arqReader = csv.DictReader(csvfile, delimiter=';')
        for line in arqReader:
            if (line.get('peso')):
                peso = float(line['peso'])
                mapaPeso.append(peso)
except Exception as err:
    print(err)
    
for i in range(0, len(mapaPeso), 7):
        print(i)
        bloco = mapaPeso[i:i+7]
        if bloco:
            resultado = sum(bloco) / len(bloco)
            semana[f"semana_{i // 7 + 1}"] = f"{resultado:.2f}"
            
            
         

for semana, media in semana.items():
    print(f"{semana}: {media}")
# print("Primeira Semana",mapaPeso[:7])
# print("Segunda Semana",mapaPeso[7:14])
# print("Media 1",sum(mapaPeso[:7])/len(mapaPeso[:7]))
# print("Media 2",sum(mapaPeso[7:14])/len(mapaPeso[7:14]))