-- Spring corre este script al arrancar y crea las secuencias que usan los repositorios
-- para generar los correlativos sin condicion de carrera (en vez de count()+1)
CREATE SEQUENCE IF NOT EXISTS seq_operacion START 1;
CREATE SEQUENCE IF NOT EXISTS seq_desembolso START 1;
CREATE SEQUENCE IF NOT EXISTS seq_cobro START 1;